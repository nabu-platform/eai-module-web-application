package be.nabu.eai.module.web.application.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import be.nabu.eai.module.http.server.RepositoryExceptionFormatter;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.events.api.ResponseHandler;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.nio.api.ExceptionFormatter;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class WebApplicationResource implements ReadableResource {

	private URI uri;

	public WebApplicationResource(URI uri) {
		this.uri = uri;
	}
	
	@Override
	public String getContentType() {
		HTTPResponse response = getResponse();
		return MimeUtils.getContentType(response.getContent().getHeaders());
	}

	@Override
	public String getName() {
		return URIUtils.getName(uri);
	}

	@Override
	public ResourceContainer<?> getParent() {
		return null;
	}

	@Override
	public ReadableContainer<ByteBuffer> getReadable() throws IOException {
		HTTPResponse response = getResponse();
		if (response == null) {
			throw new FileNotFoundException("Could not resolve: " + uri);
		}
		ModifiablePart content = response.getContent();
		if (content == null) {
			throw new FileNotFoundException("Could not find: " + uri);
		}
		return ((ContentPart) content).getReadable();
	}

	private HTTPResponse getResponse() {
		WebApplication application = (WebApplication) EAIResourceRepository.getInstance().resolve(uri.getHost());
		if (application == null) {
			throw new IllegalArgumentException("Unknown web application: " + uri.getHost());
		}
		String host = application.getConfig().getVirtualHost().getConfig().getHost();
		
		PlainMimeEmptyPart part = new PlainMimeEmptyPart(null, 
				new MimeHeader("Host", host),
				new MimeHeader("Content-Length", "0"));
		
		String target = uri.getPath();
		if (uri.getQuery() != null) {
			target += "?" + uri.getQuery();
		}
		if (uri.getFragment() != null) {
			target += "#" + uri.getFragment();
		}
		
		HTTPRequest request = new DefaultHTTPRequest("GET", URIUtils.encodeURI(target), (ModifiablePart) part);
		
		ExceptionFormatter<HTTPRequest, HTTPResponse> formatter = new RepositoryExceptionFormatter(application.getConfig().getVirtualHost().getConfig().getServer());
		HTTPResponse response = application.getDispatcher().fire(request, this, new ResponseHandler<HTTPRequest, HTTPResponse>() {
			@Override
			public HTTPResponse handle(HTTPRequest arg0, Object arg1, boolean arg2) {
				if (arg1 instanceof HTTPResponse) {
					return (HTTPResponse) arg1;
				}
				else if (arg1 instanceof Exception) {
					return formatter.format(arg0, (Exception) arg1);
				}
				return null;
			}
		}, new ResponseHandler<HTTPRequest, HTTPRequest>() {
			@Override
			public HTTPRequest handle(HTTPRequest arg0, Object arg1, boolean arg2) {
				if (arg1 instanceof HTTPRequest) {
					return (HTTPRequest) arg1;
				}
				else if (arg1 instanceof Exception) {
					throw new HTTPException(500, (Exception) arg1);
				}
				return null;
			}
		});
		return response;
	}

}
