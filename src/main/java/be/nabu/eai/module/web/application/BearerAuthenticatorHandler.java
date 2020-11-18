package be.nabu.eai.module.web.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.web.application.api.BearerAuthenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.jwt.impl.JWTBearerHandler.SimpleAuthenticationHeader;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeUtils;

public class BearerAuthenticatorHandler implements EventHandler<HTTPRequest, HTTPResponse> {

	private BearerAuthenticator authenticator;
	private String realm;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public BearerAuthenticatorHandler(BearerAuthenticator authenticator, String realm) {
		this.authenticator = authenticator;
		this.realm = realm;
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest request) {
		Header header = MimeUtils.getHeader("Authorization", request.getContent().getHeaders());
		if (header != null && header.getValue().length() >= 6 && header.getValue().substring(0, 6).equalsIgnoreCase("bearer")) {
			// should start with a space now...
			String bearer = header.getValue().substring(6).trim();
			// if you have an empty bearer value, don't call the authenticator...
			if (bearer.isEmpty()) {
				return null;
			}
			Device device = request.getContent() == null ? null : GlueListener.getDevice(realm, request.getContent().getHeaders());
			try {
				Token token = authenticator.authenticate(realm, bearer, device);
				if (token != null) {
					request.getContent().setHeader(new SimpleAuthenticationHeader(token));
				}
			}
			catch (Exception e) {
				logger.warn("Failed to parse bearer: " + bearer, e);
			}
		}
		return null;
	}

}
