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
	private WebApplication application;

	public ArbitraryAuthenticatorHandler(WebApplication application, ArbitraryAuthenticator authenticator, String realm) {
		this.application = application;
		this.authenticator = authenticator;
		this.realm = realm;
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest request) {
		Device device = request.getContent() == null ? null : GlueListener.getDevice(realm, request.getContent().getHeaders());
//		authenticator.authenticate(application.getId(), realm, request, device);
		Token token = authenticator.authenticate(application.getId(), realm, request, device);
		if (token != null) {
			request.getContent().setHeader(new SimpleAuthenticationHeader(token));
		}
		return null;
	}

}
