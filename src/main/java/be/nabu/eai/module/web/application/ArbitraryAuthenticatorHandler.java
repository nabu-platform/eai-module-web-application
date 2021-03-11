package be.nabu.eai.module.web.application;

import be.nabu.eai.module.web.application.api.ArbitraryAuthenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.jwt.impl.JWTBearerHandler.SimpleAuthenticationHeader;

public class ArbitraryAuthenticatorHandler implements EventHandler<HTTPRequest, HTTPResponse> {

	private ArbitraryAuthenticator authenticator;
	private String realm;

	public ArbitraryAuthenticatorHandler(ArbitraryAuthenticator authenticator, String realm) {
		this.authenticator = authenticator;
		this.realm = realm;
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest request) {
		Device device = request.getContent() == null ? null : GlueListener.getDevice(realm, request.getContent().getHeaders());
		authenticator.authenticate(realm, request, device);
		Token token = authenticator.authenticate(realm, request, device);
		if (token != null) {
			request.getContent().setHeader(new SimpleAuthenticationHeader(token));
		}
		return null;
	}

}
