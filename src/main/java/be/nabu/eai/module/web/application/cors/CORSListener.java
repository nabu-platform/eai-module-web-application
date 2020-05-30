package be.nabu.eai.module.web.application.cors;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.api.CORSHandler;
import be.nabu.eai.module.web.application.api.CORSResult;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class CORSListener implements EventHandler<HTTPRequest, HTTPResponse> {

	public static final String METHODS = "X-Internal-Access-Control-Allow-Methods";
	public static final String ORIGIN = "X-Internal-Access-Control-Allow-Origin";
	
	private WebApplication application;
	private CORSHandler handler;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public CORSListener(WebApplication application, CORSHandler handler) {
		this.application = application;
		this.handler = handler;
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest event) {
		// we remove any incoming header that we use to manage internal policies so they can not be injected from the outside
		event.getContent().removeHeader(ORIGIN, METHODS);
		
		Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
		// we only kick into action if there is an "Origin" header
		Header originHeader = MimeUtils.getHeader("Origin", event.getContent().getHeaders());
		if (originHeader != null && originHeader.getValue() != null && !originHeader.getValue().trim().isEmpty()) {
			String origin = originHeader.getValue();
			try {
				URI uri = HTTPUtils.getURI(event, false);
				List<String> methods = new ArrayList<String>();
				if (event.getMethod().equalsIgnoreCase("OPTIONS")) {
					// TODO: in the future we "could" scrape for actually used methods in the web application
					methods.addAll(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));
				}
				else {
					methods.add(event.getMethod().toUpperCase());
				}
				ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
				ServiceRuntime.getGlobalContext().put("service.context", application.getId());
				CORSResult check = handler.check(application.getId(), origin, uri);
				HTTPResponse response = null;
				// if we have no explicit result, we check the application, if it is from the same application we allow it by default
				// otherwise we block by default
				if (check == null) {
					URI originUri = new URI(URIUtils.encodeURI(origin, false));
					String host = application.getConfig().getVirtualHost().getConfig().getHost();
					List<String> aliases = application.getConfig().getVirtualHost().getConfig().getAliases();
					
					boolean sameHost = host == null || host.equalsIgnoreCase(originUri.getHost());
					
					if (!sameHost && aliases != null) {
						for (String alias : aliases) {
							if (alias != null && alias.equalsIgnoreCase(originUri.getHost())) {
								sameHost = true;
								break;
							}
						}
					}
					// the origin header never has a path, we can't check it against the web application path, we assume the same host is enough
					if (sameHost) {
						check = new CORSResult() {
							@Override
							public List<String> getMethods() {
								return null;
							}
							@Override
							public Boolean getAllowed() {
								return true;
							}
							@Override
							public Boolean getAllowedAll() {
								return false;
							}
						};
					}
				}
				if (check == null) {
					check = new CORSResult() {
						@Override
						public List<String> getMethods() {
							return null;
						}
						@Override
						public Boolean getAllowed() {
							return false;
						}
						@Override
						public Boolean getAllowedAll() {
							return false;
						}
					};
				}
				// if specific methods were returned, we only allow these
				if (check.getMethods() != null && !check.getMethods().isEmpty()) {
					methods = check.getMethods();
				}
				boolean isAllowed = check.getAllowed() != null && check.getAllowed();
				// for non-options, make sure the method is in there
				if (isAllowed && !event.getMethod().equalsIgnoreCase("OPTIONS")) {
					isAllowed = false;
					for (String method : methods) {
						if (method.equalsIgnoreCase(event.getMethod())) {
							isAllowed = true;
							break;
						}
					}
				}
				boolean isAllowedAll = check.getAllowedAll() != null && check.getAllowedAll();
				if (isAllowed) {
					// if it was an options request, we need to send back a response immediately
					if (event.getMethod().equalsIgnoreCase("OPTIONS")) {
						response = new DefaultHTTPResponse(event, 204, HTTPCodes.getMessage(204), new PlainMimeEmptyPart(null, 
							new MimeHeader("Content-Length", "0"),
							new MimeHeader("Access-Control-Allow-Origin", isAllowedAll ? "*" : origin),
							new MimeHeader("Access-Control-Allow-Methods", methods.toString().replace("[", "").replace("]", "")
						)));
						if (!isAllowedAll) {
							response.getContent().setHeader(new MimeHeader("Vary", "Origin"));
						}
					}
					// we enrich the original request with internal headers that we can later inject in the response with post processor
					else {
						event.getContent().setHeader(new MimeHeader(ORIGIN, isAllowedAll ? "*" : origin));
						event.getContent().setHeader(new MimeHeader(METHODS, methods.toString().replace("[", "").replace("]", "")));
					}
				}
				// if you are not allowed, we send this back immediately
				else {
					response = new DefaultHTTPResponse(event, 403, HTTPCodes.getMessage(403), new PlainMimeEmptyPart(null, 
						new MimeHeader("Content-Length", "0")));
				}
				return response;
			}
			catch (Exception e) {
				throw new HTTPException(500, e);
			}
			finally {
				ServiceRuntime.setGlobalContext(originalContext);
			}
		}
		return null;
	}

}
