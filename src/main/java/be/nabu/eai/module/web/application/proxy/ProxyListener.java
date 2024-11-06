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

package be.nabu.eai.module.web.application.proxy;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.types.binding.api.UnmarshallableBinding;

/**
 * You can set up a proxy per endpoint (one per rest service for example)
 * In this case rewriting and validation can be performed (or simply a new interface that provides a binding in response to a request?)
 *
 * The proxy should allow for automatic zipping (strip the header when sending it to the backend, but respect it when returning to the frontend)
 * Proxy should allow for caching: if a cache provider is set, check the cache headers?
 * 		> this can be slightly harder to accomplish because caching in the backend considers language, input parameters...
 * 		> this is context we don't have, we only have the raw request
 * 		> however for GET requests (or any request without a body) we "could" perform caching based on certain headers
 * 		> not all headers as this would generate nearly unique cache entries...
 * 		> hard to cache with only last modified or etag
 * 
 * avoid caching cookie-based responses
 */
public class ProxyListener implements EventHandler<HTTPRequest, HTTPResponse> {

	private UnmarshallableBinding binding;
	private WebApplication application;
	private HTTPClient client;
	private String remoteHost;
	private boolean remoteSecure;

	public ProxyListener(WebApplication application, UnmarshallableBinding binding, HTTPClient client, String remoteHost, boolean remoteSecure) {
		this.application = application;
		this.binding = binding;
		this.client = client;
		this.remoteHost = remoteHost;
		this.remoteSecure = remoteSecure;
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest event) {
		// if the application has an authenticator, authenticate it and set a jwt token with a key that is trusted
		// need additional awareness of who can request what
		// for example you should only be able to request your own data, but that requires knowing the user and how he relates to the data
		// pipe the request to the target using a given http client
		try {
			return client.execute(event, null, remoteSecure, false);
		}
		catch (Exception e) {
			throw new HTTPException(500, e);
		}
	}
	
}
