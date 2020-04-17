package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nabu.web.application.types.PropertyImpl;
import nabu.web.application.types.WebApplicationInformation;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.module.web.application.rate.RateLimiter;
import be.nabu.eai.repository.api.FeaturedExecutionContext;
import be.nabu.eai.repository.api.LanguageProvider;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.DeviceValidator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.AuthenticationHeader;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.core.ServerHeader;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.Pipeline;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeUtils;

public class WebApplicationUtils {
	
	public static void featureRich(WebApplication application, HTTPRequest request, ExecutionContext context) {
		Header header = MimeUtils.getHeader("Feature", request.getContent().getHeaders());
		if (header != null && header.getValue() != null && context instanceof FeaturedExecutionContext) {
			for (String feature : header.getValue().split("[\\s]*;[\\s]*")) {
				int index = feature.lastIndexOf('=');
				boolean enable = index < 0 || feature.substring(index + 1).equals("true");
				if (index >= 0) {
					feature = feature.substring(0, index);
				}
				// if we can't test the feature, we leave it alone
				if (application.canTestFeature(context.getSecurityContext().getToken(), feature)) {
					if (enable && !((FeaturedExecutionContext) context).getEnabledFeatures().contains(feature)) {
						((FeaturedExecutionContext) context).getEnabledFeatures().add(feature);
					}
					else if (!enable) {
						((FeaturedExecutionContext) context).getEnabledFeatures().remove(feature);
					}
				}
			}
		}
	}
	
	public static void limitToApplication(EventSubscription<HTTPRequest, ?> subscription, WebApplication application) {
		if (application.getConfig().getPath() != null) {
			subscription.filter(HTTPServerUtils.limitToPath(application.getConfig().getPath()));
		}
	}
	
	public static Source getSource() {
		Pipeline pipeline = PipelineUtils.getPipeline();
		return pipeline == null ? null : new SourceImpl(pipeline.getSourceContext());
	}

	public static String getApplicationLanguage(WebApplication application, HTTPRequest request) throws IOException {
		String language = null;
		if (language == null && application.getRequestLanguageProvider() != null) {
			language = application.getRequestLanguageProvider().getLanguage(request);
		}
		return language;
	}
	
	public static String getProxyPath(WebApplication application, HTTPRequest request) {
		Header header = MimeUtils.getHeader(ServerHeader.PROXY_PATH.getName(), request.getContent().getHeaders());
		if (header != null && header.getValue() != null) {
			return header.getValue();
		}
		return application.getConfig().getProxyPath();
	}
	
	public static boolean refererMatches(WebApplication application, URI referer) {
		boolean refererMatch = false;
		if (referer != null) {
			VirtualHostArtifact virtualHost = application.getConfig().getVirtualHost();
			if (referer.getHost() != null) {
				refererMatch = referer.getHost().equals(virtualHost.getConfig().getHost());
				if (!refererMatch) {
					List<String> aliases = virtualHost.getConfig().getAliases();
					if (aliases != null) {
						for (String alias : aliases) {
							refererMatch = referer.getHost().equals(alias);
							if (refererMatch) {
								break;
							}
						}
					}
				}
			}
			else {
				refererMatch = true;
			}
		}
		return refererMatch;
	}
	
	public static String getLanguage(WebApplication application, HTTPRequest request) throws IOException {
		// if we don't have a request, we can't deduce a language, this can be the case for example in cordova building
		if (request == null) {
			return null;
		}
		String language = null;
		// first get it from the language provider (if any)
		if (application.getUserLanguageProvider() != null) {
			language = application.getUserLanguageProvider().getLanguage(getToken(application, request));
		}
		// then try to get it from cookies, this mechanism can be used for anonymous users with a personal preference
		if (language == null && !application.getConfig().isIgnoreLanguageCookie()) {
			Map<String, List<String>> cookies = HTTPUtils.getCookies(request.getContent().getHeaders());
			if (cookies != null && cookies.get("language") != null && !cookies.get("language").isEmpty()) {
				language = cookies.get("language").get(0);
			}
		}
		if (language == null && application.getConfig().isForceRequestLanguage() && application.getRequestLanguageProvider() != null) {
			language = application.getRequestLanguageProvider().getLanguage(request);
		}
		// try to get it from the browser preferences, this can be used for anonymous users without a personal preference
		if (language == null) {
			List<String> supportedLanguages = null;
			List<String> acceptedLanguages = MimeUtils.getAcceptedLanguages(request.getContent().getHeaders());
			if (!acceptedLanguages.isEmpty()) {
				LanguageProvider provider = application.getLanguageProvider();
				supportedLanguages = provider == null ? null : provider.getSupportedLanguages();
				for (String acceptedLanguage : acceptedLanguages) {
					String potential = acceptedLanguage.replaceAll("-.*$", "");
					if (supportedLanguages == null || supportedLanguages.contains(potential)) {
						language = potential;
						break;
					}
				}
			}
		}
		// try to get it from the request
		if (language == null && application.getRequestLanguageProvider() != null && !application.getConfig().isForceRequestLanguage()) {
			language = application.getRequestLanguageProvider().getLanguage(request);
		}
		// if you have configured a default, send that back
		if (language == null) {
			language = application.getConfig().getDefaultLanguage();
		}
		return language;
	}
	
	public static WebApplicationInformation getInformation(WebApplication application) {
		WebApplicationInformation information = new WebApplicationInformation();
		information.setId(application.getId());
		information.setRealm(application.getRealm());
		information.setHtml5Mode(application.getConfig().isHtml5Mode());
		information.setCharset(application.getConfig().getCharset() == null ? Charset.defaultCharset() : Charset.forName(application.getConfig().getCharset()));
		information.setCookiePath(application.getCookiePath());
		// the default error code is HTTP-500
		information.setErrorCodes(new ArrayList<String>(Arrays.asList("HTTP-*", "HTTP-400", "HTTP-401", "HTTP-403", "HTTP-404", "HTTP-429", "HTTP-500", "HTTP-502", "HTTP-503")));
		if (application.getConfig().getWhitelistedCodes() != null) {
			for (String code : application.getConfig().getWhitelistedCodes().split("[\\s]*,[\\s]*")) {
				int from = code.indexOf('(');
				int to = code.indexOf(')', from);
				if (from >= 0 && to >= 0) {
					information.getErrorCodes().add(code.substring(from + 1, to));
				}
				else {
					information.getErrorCodes().add(code);
				}
			}
		}
		if (application.getConfig().getVirtualHost() != null) {
			information.setHost(application.getConfig().getVirtualHost().getConfig().getHost());
			information.setAliases(application.getConfig().getVirtualHost().getConfig().getAliases());
			if (application.getConfig().getVirtualHost().getConfig().getServer() != null) {
				HTTPServerArtifact server = application.getConfig().getVirtualHost().getConfig().getServer();
				information.setPort(server.getConfig().isProxied() ? server.getConfig().getProxyPort() : server.getConfig().getPort());
				information.setSecure(server.isSecure());
				if (information.getPort() == null) {
					information.setPort(server.isSecure() ? 443 : 80);
				}
				information.setScheme(information.getSecure() ? "https" : "http");
			}
		}
		information.setPath(application.getConfig().getPath());
		if (application.getConfig().getTranslationService() != null) {
			information.setTranslationService(application.getConfig().getTranslationService().getId());
		}
		// if the application has not been started (e.g. no virtual host or it is stopped), there is no listener
		if (application.getListener() != null) {
			Map<String, String> properties = application.getListener().getEnvironment().getParameters();
			for (String key : properties.keySet()) {
				information.getProperties().add(new PropertyImpl(key, properties.get(key)));
			}
		}
		if (application.getConfig().getScriptCacheProvider() != null) {
			information.setScriptCacheProviderId(application.getConfig().getScriptCacheProvider().getId());
		}
		return information;
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
				throw new HTTPException(token == null ? 401 : 403, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' is using an unauthorized device '" + device.getDeviceId() + "'", token);
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
	
	public static void checkPermission(WebApplication application, Token token, String permissionAction, String permissionContext) {
		try {
			PermissionHandler permissionHandler = application.getPermissionHandler();
			if (permissionHandler != null && permissionAction != null) {
				if (!permissionHandler.hasPermission(token, permissionContext, permissionAction)) {
					throw new HTTPException(token == null ? 401 : 403, "User does not have permission to execute the rest service", "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' does not have permission to '" + permissionAction + "' on: " + permissionContext, token);
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
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
					throw new HTTPException(token == null ? 401 : 403, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' does not have one of the allowed roles '" + roles + "'", token);
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
