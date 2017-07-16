package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.module.web.application.rate.RateLimiter;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.DeviceValidator;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.AuthenticationHeader;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.Pipeline;

public class WebApplicationUtils {
	
	public static Source getSource() {
		Pipeline pipeline = PipelineUtils.getPipeline();
		return pipeline == null ? null : new SourceImpl(pipeline.getSourceContext());
	}
	
	public static Session getSession(WebApplication application, HTTPRequest request) {
		Map<String, List<String>> cookies = HTTPUtils.getCookies(request.getContent().getHeaders());
		String originalSessionId = GlueListener.getSessionId(cookies);
		return originalSessionId == null ? null : application.getSessionProvider().getSession(originalSessionId);
	}
	
	public static Token getToken(WebApplication application, HTTPRequest request) {
		Map<String, List<String>> cookies = HTTPUtils.getCookies(request.getContent().getHeaders());
		String originalSessionId = GlueListener.getSessionId(cookies);
		Session session = originalSessionId == null ? null : application.getSessionProvider().getSession(originalSessionId);
		
		// authentication tokens in the request get precedence over session-based authentication
		AuthenticationHeader authenticationHeader = HTTPUtils.getAuthenticationHeader(request);
		Token token = authenticationHeader == null ? null : authenticationHeader.getToken();
		// but likely we'll have to check the session for tokens
		if (token == null && session != null) {
			token = (Token) session.get(GlueListener.buildTokenName(application.getRealm()));
		}
		else if (token != null && session != null) {
			session.set(GlueListener.buildTokenName(application.getRealm()), token);
		}
		try {
			// check validity of token
			TokenValidator tokenValidator = application.getTokenValidator();
			if (tokenValidator != null) {
				if (token != null && !tokenValidator.isValid(token)) {
					session.destroy();
					originalSessionId = null;
					session = null;
					token = null;
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return token;
	}
	
	public static Device getDevice(WebApplication application, HTTPRequest request, Token token) {
		try {
			DeviceValidator deviceValidator = application.getDeviceValidator();
			// check validity of device
			Device device = request.getContent() == null ? null : GlueListener.getDevice(application.getRealm(), request.getContent().getHeaders());
			if (device == null && deviceValidator != null) {
				device = GlueListener.newDevice(application.getRealm(), request.getContent().getHeaders());
			}
			if (deviceValidator != null && !deviceValidator.isAllowed(token, device)) {
				throw new HTTPException(token == null ? 401 : 403, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' is using an unauthorized device '" + device.getDeviceId() + "'");
			}
			return device;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static boolean isNewDevice(WebApplication application, HTTPRequest request) {
		return request.getContent() == null ? true : GlueListener.getDevice(application.getRealm(), request.getContent().getHeaders()) == null;
	}
	
	public static void checkRole(WebApplication application, Token token, List<String> roles) {
		try {
			RoleHandler roleHandler = application.getRoleHandler();
			if (roleHandler != null && roles != null) {
				boolean hasRole = false;
				for (String role : roles) {
					if (roleHandler.hasRole(token, role)) {
						hasRole = true;
						break;
					}
				}
				if (!hasRole) {
					throw new HTTPException(token == null ? 401 : 403, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' does not have one of the allowed roles '" + roles + "'");
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static HTTPResponse checkRateLimits(WebApplication application, Token token, Device device, String action, String context, HTTPRequest request) {
		try {
			// check rate limiting (if any)
			RateLimiter rateLimiter = application.getRateLimiter();
			if (rateLimiter != null) {
				HTTPResponse response = rateLimiter.handle(application, request, new SourceImpl(PipelineUtils.getPipeline().getSourceContext()), token, device, action, context);
				if (response != null) {
					return response;
				}
			}
			return null;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getFragmentPath(WebApplication application, String relativePath, String childPath) {
		try {
			String fullPath = application.getServerPath();
			fullPath = relativize(fullPath, relativePath);
			fullPath = relativize(fullPath, childPath);
			return fullPath;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String relativize(String fullPath, String childPath) {
		if (childPath != null && !childPath.isEmpty() && !childPath.equals("/")) {
			if (!fullPath.endsWith("/")) {
				fullPath += "/";
			}
			fullPath += childPath.replaceFirst("^[/]+", "");
		}
		return fullPath;
	}
}
