/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
	private WebApplication application;

	public BearerAuthenticatorHandler(WebApplication application, BearerAuthenticator authenticator, String realm) {
		this.application = application;
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
				Token token = authenticator.authenticate(application.getId(), realm, bearer, device);
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
