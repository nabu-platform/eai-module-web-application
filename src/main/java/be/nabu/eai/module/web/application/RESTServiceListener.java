package be.nabu.eai.module.web.application;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.impl.ResponseMethods;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.api.UnmarshallableBinding;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.mask.MaskedContent;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.Validator;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class RESTServiceListener implements EventHandler<HTTPRequest, HTTPResponse> {

	private DefinedService service;
	private WebApplication application;
	private ServiceRESTFragment fragment;

	public RESTServiceListener(WebApplication application, ServiceRESTFragment fragment, DefinedService service) {
		this.application = application;
		this.fragment = fragment;
		this.service = service;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public HTTPResponse handle(HTTPRequest request) {
		Token token = null;
		Device device = null;
		try {
			ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
			ServiceRuntime.getGlobalContext().put("service.context", application.getId());
			// fail fast if wrong method
			if (!fragment.getMethod().equals(request.getMethod())) {
				return null;
			}
			URI uri = HTTPUtils.getURI(request, false);
			String path = URIUtils.normalize(uri.getPath());
			// not in this web artifact
			String serverPath = (application.getServerPath() + "/" + fragment.getPath()).replaceAll("[/]{2,}", "/");
			if (!path.equals(serverPath)) {
				return null;
			}

			token = WebApplicationUtils.getToken(application, request);
			device = WebApplicationUtils.getDevice(application, request, token);

			ServiceRuntime.getGlobalContext().put("device", device);
			
			HTTPResponse checkRateLimits = WebApplicationUtils.checkRateLimits(application, token, device, service.getId(), null, request);
			if (checkRateLimits != null) {
				return checkRateLimits;
			}
			
			WebApplicationUtils.checkPermission(application, token, service.getId(), null);
			
			ComplexType inputType = fragment.getScopedInput();
			ComplexContent input = null;
			if (fragment.getMethod().equals("GET")) {
				input = inputType.newInstance();
				Map<String, List<String>> queryProperties = URIUtils.getQueryProperties(uri);
				for (String key : queryProperties.keySet()) {
					List<String> list = queryProperties.get(key);
					if (list != null && !list.isEmpty()) {
						input.set(key.replace(":", "/"), list.get(0));
					}
				}
			}
			else {
				String contentType = MimeUtils.getContentType(request.getContent().getHeaders());
				UnmarshallableBinding binding = null;
				if ("application/xml".equals(contentType)) {
					binding = new XMLBinding(inputType, Charset.forName("UTF-8"));
					((XMLBinding) binding).setIgnoreUndefined(true);
				}
				else if ("application/json".equals(contentType)) {
					binding = new JSONBinding(inputType, Charset.forName("UTF-8"));
					((JSONBinding) binding).setIgnoreUnknownElements(true);
				}
				else {
					throw new HTTPException(415, "Unsupported request content type", "Unsupported request content type: " + contentType, token);
				}
				ReadableContainer<ByteBuffer> readable = ((ContentPart) request.getContent()).getReadable();
				if (readable == null) {
					throw new HTTPException(400, "Expecting input", token);
				}
				try {
					input = binding.unmarshal(IOUtils.toInputStream(readable), new Window[0]);
				}
				finally {
					readable.close();
				}
			}
			Validator validator = inputType.createValidator();
			List<? extends Validation<?>> validations = validator.validate(input);
			if (validations != null && !validations.isEmpty()) {
				throw new HTTPException(400, "Invalid request", token);
			}
			
			ExecutionContext newExecutionContext = application.getRepository().newExecutionContext(token);
			ServiceRuntime runtime = new ServiceRuntime(service, newExecutionContext);
			// we can use the tracker to report our HTTP shizzles to anyone who might be listening
			ServiceRuntimeTracker tracker = newExecutionContext.getServiceContext().getServiceTrackerProvider().getTracker(runtime);
			if (tracker != null) {
				tracker.report(HTTPUtils.toMessage(request));
			}
			ComplexContent output = runtime.run(input);

			HTTPResponse response;
			if (output != null) {
				// mask it to the restricted type
				if (!output.getType().equals(fragment.getOutput())) {
					output = new MaskedContent(output, fragment.getOutput());
				}
				
				List<String> acceptedContentTypes = request.getContent() != null
						? MimeUtils.getAcceptedContentTypes(request.getContent().getHeaders())
						: new ArrayList<String>();
				acceptedContentTypes.retainAll(ResponseMethods.allowedTypes);
				String contentType = acceptedContentTypes.isEmpty() ? "application/json" : acceptedContentTypes.get(0);
				MarshallableBinding binding = null;
				if (contentType.equalsIgnoreCase("application/xml")) {
					// XML can't handle multiple roots, so we leave the wrapper in place in case we have a root array
					binding = new XMLBinding(output.getType(), Charset.forName("UTF-8"));
				}
				else if (contentType.equalsIgnoreCase("application/json")) {
					binding = new JSONBinding(output.getType(), Charset.forName("UTF-8"));
					((JSONBinding) binding).setIgnoreRootIfArrayWrapper(true);
				}
				else {
					throw new HTTPException(500, "Unsupported response content type: " + contentType);
				}
				ByteArrayOutputStream content = new ByteArrayOutputStream();
				binding.marshal(content, (ComplexContent) output);
				byte[] byteArray = content.toByteArray();
				List<Header> headers = new ArrayList<Header>();
				headers.add(new MimeHeader("Content-Length", "" + byteArray.length));
				headers.add(new MimeHeader("Content-Type", contentType + "; charset=UTF-8"));
				PlainMimeContentPart part = new PlainMimeContentPart(null,
					IOUtils.wrap(byteArray, true),
					headers.toArray(new Header[headers.size()])
				);
				HTTPUtils.setContentEncoding(part, request.getContent().getHeaders());
				response = new DefaultHTTPResponse(request, 200, HTTPCodes.getMessage(200), part);
			}
			else {
				response = new DefaultHTTPResponse(request, 200, "OK", new PlainMimeEmptyPart(null,
					new MimeHeader("Content-Length", "0")
				));
			}
			if (tracker != null) {
				tracker.report(HTTPUtils.toMessage(response));
			}
			return response;
		}
		catch (HTTPException e) {
			if (e.getToken() == null) {
				e.setToken(token);
			}
			if (e.getDevice() == null) {
				e.setDevice(device);
			}
			e.getContext().addAll(Arrays.asList(application.getId(), service.getId()));
			throw e;
		}
		catch (Exception e) {
			HTTPException httpException = new HTTPException(500, "Could not execute service", "Could not execute service: " + service.getId(), e, token);
			httpException.getContext().addAll(Arrays.asList(application.getId(), service.getId()));
			httpException.setDevice(device);
			throw httpException;
		}
		finally {
			ServiceRuntime.setGlobalContext(null);
		}
	}

	public static ServiceRESTFragment asRestFragment(WebApplication application, DefinedService service) {
		return new ServiceRESTFragment(service, application);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void buildQueryParameters(ComplexType type, List<ComplexType> checked, String path, List<Element<?>> parameters) {
		for (Element<?> child : TypeUtils.getAllChildren(type)) {
			String childPath = (path == null ? "" : path + ":") + child.getName();
			if (child.getType() instanceof SimpleType) {
				parameters.add(new SimpleElementImpl(childPath, (SimpleType) child.getType(), type, child.getProperties()));
			}
			else if (child.getType() instanceof ComplexType) {
				buildQueryParameters((ComplexType) child.getType(), checked, childPath, parameters);
			}
		}
	}
	
	// if all input fields are simple types _and_ optional, we can expose it as a GET service
	static boolean isGetCompatible(ComplexType type, List<ComplexType> checked) {
		// protect against circular
		if (checked.contains(type)) {
			return true;
		}
		checked.add(type);
		for (Element<?> element : TypeUtils.getAllChildren(type)) {
			// any mandatory element and you're out
			if (ValueUtils.getValue(MinOccursProperty.getInstance(), element.getProperties()) > 0) {
				return false;
			}
			// if it is a complex type, check if it is a list
			if (element.getType() instanceof ComplexType) {
				if (element.getType().isList(element.getProperties())) {
					return false;
				}
				// otherwise we recurse
				else {
					if (!isGetCompatible((ComplexType) element.getType(), checked)) {
						return false;
					}
				}
			}
		}
		return true;
	}
}
