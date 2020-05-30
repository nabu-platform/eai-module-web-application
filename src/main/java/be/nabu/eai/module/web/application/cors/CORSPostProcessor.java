package be.nabu.eai.module.web.application.cors;

import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.LinkableHTTPResponse;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;

public class CORSPostProcessor implements EventHandler<HTTPResponse, HTTPResponse> {

	@Override
	public HTTPResponse handle(HTTPResponse event) {
		if (event instanceof LinkableHTTPResponse) {
			HTTPRequest request = ((LinkableHTTPResponse) event).getRequest();
			if (request != null) {
				Header originHeader = MimeUtils.getHeader(CORSListener.ORIGIN, request.getContent().getHeaders());
				Header methodsHeader = MimeUtils.getHeader(CORSListener.METHODS, request.getContent().getHeaders());
				if (originHeader != null) {
					event.getContent().setHeader(new MimeHeader("Access-Control-Allow-Origin", originHeader.getValue()));
					// if it is not allowed to all, make sure we signify the vary of the origin
					if (!"*".equals(originHeader.getValue())) {
						event.getContent().setHeader(new MimeHeader("Vary", "Origin"));
					}
				}
				if (methodsHeader != null) {
					event.getContent().setHeader(new MimeHeader("Access-Control-Allow-Methods", methodsHeader.getValue()));
				}
			}
		}
		return null;
	}

}
