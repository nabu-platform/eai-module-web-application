package be.nabu.eai.module.web.application.api;

import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;

public interface RequestHandler {
	public HTTPResponse handle(WebApplication application, HTTPRequest request, Source source, Token token, Device device, String action, String context);
}
