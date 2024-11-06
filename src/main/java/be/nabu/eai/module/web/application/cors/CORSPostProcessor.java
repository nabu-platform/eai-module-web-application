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
				Header credentialsHeader = MimeUtils.getHeader(CORSListener.CREDENTIALS, request.getContent().getHeaders());
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
				if (credentialsHeader != null && credentialsHeader.getValue().equals("true")) {
					event.getContent().setHeader(new MimeHeader("Access-Control-Allow-Credentials", credentialsHeader.getValue()));
				}
			}
		}
		return null;
	}

}
