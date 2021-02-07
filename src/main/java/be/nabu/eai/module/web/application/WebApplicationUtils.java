package be.nabu.eai.module.web.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nabu.web.application.types.PropertyImpl;
import nabu.web.application.types.WebApplicationInformation;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.module.web.application.api.RateLimitCheck;
import be.nabu.eai.module.web.application.api.RateLimitProvider;
import be.nabu.eai.module.web.application.api.RateLimitSettings;
import be.nabu.eai.repository.api.LanguageProvider;
import be.nabu.eai.repository.api.VirusInfection;
import be.nabu.eai.repository.api.VirusScanner;
import be.nabu.eai.server.Server;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.DeviceValidator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPEntity;
import be.nabu.libs.http.api.HTTPInterceptor;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.AuthenticationHeader;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.core.ServerHeader;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.impl.ResponseMethods;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.Pipeline;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.FeaturedExecutionContext;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceRuntimeTracker;
import be.nabu.libs.types.CollectionHandlerFactory;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.form.FormBinding;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.utils.cep.api.EventSeverity;
import be.nabu.utils.cep.impl.CEPUtils;
import be.nabu.utils.cep.impl.HTTPComplexEventImpl;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class WebApplicationUtils {
	
	private static Boolean TRACK_REQUESTS = Boolean.parseBoolean(System.getProperty("http.track", "true"));
	
	public static HTTPInterceptor getInterceptor(WebApplication application, ServiceRuntime runtime) {
		ServiceRuntimeTracker tracker = TRACK_REQUESTS ? runtime.getExecutionContext().getServiceContext().getServiceTrackerProvider().getTracker(runtime) : null;
		if (tracker != null) {
			return new HTTPInterceptor() {
				@Override
				public HTTPEntity intercept(HTTPEntity entity) {
					tracker.report(HTTPUtils.toMessage(entity));
					return entity;
				}
			};
		}
		else {
			return null;
		}
	}
	
	// if we allow (some) headers as query parameter, override the request
	// we currently allow you to fixate the "accept" to determine the return type, the language and the content disposition (to force a download)
	public static void queryToHeader(HTTPRequest request, Map<String, List<String>> queryProperties) throws ParseException, IOException {
		for (String key : queryProperties.keySet()) {
			if (key.startsWith("header:") && !queryProperties.get(key).isEmpty()) {
				String headerName = key.substring("header:".length());
				for (String allowed : Arrays.asList("Accept", "Accept-Language", "Accept-Content-Disposition")) {
					if (headerName.equalsIgnoreCase(allowed)) {
						request.getContent().removeHeader(allowed);
						request.getContent().setHeader(MimeHeader.parseHeader(allowed + ":" + queryProperties.get(key).get(0)));
						break;
					}
				}
			}
		}
	}
	
	public static void featureRich(WebApplication application, HTTPRequest request, ExecutionContext context) {
		Header header = MimeUtils.getHeader("Feature", request.getContent().getHeaders());
		if (header != null && header.getValue() != null && context instanceof FeaturedExecutionContext) {
			for (String feature : MimeUtils.getFullHeaderValue(header).split("[\\s]*;[\\s]*")) {
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
	
	public static boolean refererMatches(WebApplication application, HTTPRequest request) {
		try {
			Header refererHeader = MimeUtils.getHeader("Referer", request.getContent().getHeaders());
			URI referer = refererHeader == null ? null : new URI(URIUtils.encodeURI(refererHeader.getValue()));
			return refererMatches(application, referer);
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public static boolean refererMatches(WebApplication application, URI referer) {
		boolean refererMatch = false;
		if (referer != null) {
			VirtualHostArtifact virtualHost = application.getConfig().getVirtualHost();
			// if we did not define a host in the virtual host, we don't care!
			if (virtualHost.getConfig().getHost() != null) {
				refererMatch = referer.getHost() != null && referer.getHost().equals(virtualHost.getConfig().getHost());
				if (!refererMatch && referer.getHost() != null) {
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
	
	public static void checkOffline(WebApplication application, HTTPRequest request) {
		if (application.getRepository().getServiceRunner() instanceof Server) {
			// if we are offline but the http server is still "online" (so it has no dedicated offline port), we throw exceptions
			if (((Server) application.getRepository().getServiceRunner()).isOffline()) {
				VirtualHostArtifact host = application.getConfig().getVirtualHost();
				if (host != null) {
					HTTPServerArtifact server = host.getConfig().getServer();
					if (server != null) {
						if (server.getConfig().getOfflinePort() == null) {
							throw new HTTPException(503, "Server is running in offline modus");
						}
					}
				}
			}
		}
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
		Map<String, List<String>> cookies = refererMatches(application, request) ? HTTPUtils.getCookies(request.getContent().getHeaders()) : new HashMap<String, List<String>>();
		String originalSessionId = GlueListener.getSessionId(cookies);
		return originalSessionId == null ? null : application.getSessionProvider().getSession(originalSessionId);
	}
	
	public static Token getToken(WebApplication application, HTTPRequest request) {
		Map<String, List<String>> cookies = refererMatches(application, request) ? HTTPUtils.getCookies(request.getContent().getHeaders()) : new HashMap<String, List<String>>();
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
		Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
		try {
			ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
			ServiceRuntime.getGlobalContext().put("service.context", application.getId());
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
		finally {
			ServiceRuntime.setGlobalContext(originalContext);
		}
		return token;
	}
	
	public static Device getDevice(WebApplication application, HTTPRequest request, Token token) {
		Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
		try {
			ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
			ServiceRuntime.getGlobalContext().put("service.context", application.getId());
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
		finally {
			ServiceRuntime.setGlobalContext(originalContext);
		}
	}
	
	public static boolean isNewDevice(WebApplication application, HTTPRequest request) {
		return request.getContent() == null ? true : GlueListener.getDevice(application.getRealm(), request.getContent().getHeaders()) == null;
	}
	
	public static void checkPermission(WebApplication application, Token token, String permissionAction, String permissionContext) {
		Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
		try {
			ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
			ServiceRuntime.getGlobalContext().put("service.context", application.getId());
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
		finally {
			ServiceRuntime.setGlobalContext(originalContext);
		}
	}
	
	public static void checkRole(WebApplication application, Token token, List<String> roles) {
		Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
		try {
			ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
			ServiceRuntime.getGlobalContext().put("service.context", application.getId());
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
		finally {
			ServiceRuntime.setGlobalContext(originalContext);
		}
	}
	
	public static HTTPResponse checkRateLimits(WebApplication application, Token token, Device device, String action, String context, HTTPRequest request) throws IOException {
		SourceImpl source = new SourceImpl(PipelineUtils.getPipeline().getSourceContext());
		return checkRateLimits(application, source, token, device, action, context, request);
	}
	
	public static HTTPResponse checkRateLimits(WebApplication application, Source source, Token token, Device device, String action, String context, HTTPRequest request) throws IOException {
		// if we are being proxied, get the "actual" source
		if (request.getContent() != null && application.getConfig().getVirtualHost().getConfig().getServer().getConfig().isProxied()) {
			SourceImpl actualSource = new SourceImpl();
			actualSource.setRemoteHost(HTTPUtils.getRemoteHost(application.getConfig().getVirtualHost().getConfig().getServer().getConfig().isProxied(), request.getContent().getHeaders()));
			actualSource.setRemoteIp(HTTPUtils.getRemoteAddress(application.getConfig().getVirtualHost().getConfig().getServer().getConfig().isProxied(), request.getContent().getHeaders()));
			Header header = MimeUtils.getHeader(ServerHeader.REMOTE_PORT.getName(), request.getContent().getHeaders());
			if (header != null && header.getValue() != null) {
				actualSource.setRemotePort(Integer.parseInt(header.getValue()));
			}
			header = MimeUtils.getHeader(ServerHeader.LOCAL_PORT.getName(), request.getContent().getHeaders());
			if (header != null && header.getValue() != null) {
				actualSource.setLocalPort(Integer.parseInt(header.getValue()));
			}
			source = actualSource;
		}
		
		RateLimitProvider rateLimiter = application.getRateLimiter();
		if (rateLimiter != null) {
			Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
			try {
				ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
				ServiceRuntime.getGlobalContext().put("service.context", application.getId());
				List<RateLimitSettings> settings = rateLimiter.settings(application.getId(), source, token, device, action, context);
				if (settings != null && !settings.isEmpty()) {
						for (RateLimitSettings setting : settings) {
							// we need, at the very least, a limiting amount to be useful
							if (setting == null || setting.getAmount() == null) {
								continue;
							}
							long time = new Date().getTime();
							// if the interval is null, you just get a fixed amount of calls
							RateLimitCheck check = rateLimiter.check(application.getId(), setting.getRuleId(), setting.getIdentity(), setting.getContext(), setting.getInterval() == null ? null : new Date(time - setting.getInterval()));
							// if we get a result, let's validate it
							if (check != null && check.getAmountOfHits() != null) {
								// the amount of hits we get here are already done, so the current hit is +1. That's why we check >=.
								if (check.getAmountOfHits() >= setting.getAmount()) {
									PlainMimeEmptyPart content = new PlainMimeEmptyPart(null, 
										new MimeHeader("Content-Length", "0"));
									Long millisecondsUntilFreeSlot = null;
									// let's see if we can determine until when
									if (check.getOldestHit() != null && setting.getInterval() != null) {
										millisecondsUntilFreeSlot = setting.getInterval() - (time - check.getOldestHit().getTime());
										content.setHeader(new MimeHeader("Retry-After", "" + (millisecondsUntilFreeSlot / 1000)));
									}
									sendEvent(application, source, token, device, action, context, request, setting.getRuleId(), setting.getRuleCode(), check.getAmountOfHits(), millisecondsUntilFreeSlot);
									return new DefaultHTTPResponse(request, 429, HTTPCodes.getMessage(429), content);
								}
							}
							// if we make it past the above check, log a hit
							rateLimiter.log(application.getId(), setting.getRuleId(), setting.getIdentity(), setting.getContext(), new Date(time));
						}
				}
			}
			finally {
				ServiceRuntime.setGlobalContext(originalContext);
			}
		}
		return null;
	}
	
	private static void sendEvent(WebApplication application, Source source, Token token, Device device, String action, String context, HTTPRequest request, String ruleId, String ruleCode, Integer amount, Long duration) {
		if (application.getRepository().getComplexEventDispatcher() != null) {
			HTTPComplexEventImpl event = new HTTPComplexEventImpl();
			event.setArtifactId(application.getId());
			event.setEventName("rate-limit-hit");
			event.setEventCategory("rate-limit");
			event.setMethod(request.getMethod());
			Header header = MimeUtils.getHeader("User-Agent", request.getContent().getHeaders());
			if (header != null) {
				event.setUserAgent(MimeUtils.getFullHeaderValue(header));
			}
			
			if (source != null) {
				event.setSourceHost(source.getRemoteHost());
				event.setSourceIp(source.getRemoteIp());
				event.setSourcePort(source.getRemotePort());
				event.setDestinationPort(source.getLocalPort());
			}
			event.setTransportProtocol("TCP");
			event.setApplicationProtocol(application.getConfig().getVirtualHost().getConfig().getServer().isSecure() ? "HTTPS" : "HTTP");
			event.setSizeIn(MimeUtils.getContentLength(request.getContent().getHeaders()));
			try {
				event.setRequestUri(HTTPUtils.getURI(request, event.getApplicationProtocol().equals("HTTPS")));
			}
			catch (FormatException e) {
				// ignore
			}
			event.setCreated(new Date());
			event.setCode(ruleCode);
			event.setSeverity(EventSeverity.ERROR);
			event.setResponseCode(429);
			event.setAction(action);
			event.setContext(context);
			event.setCorrelationId(ruleId);
			if (device != null) {
				event.setDeviceId(device.getDeviceId());
			}
			if (token != null) {
				event.setRealm(token.getRealm());
				event.setAlias(token.getName());
			}
			event.setReason("limit reached: " + amount + ", try again in: " + duration + "ms");
			// this is very misleading, it seems like the event itself took that long
//			event.setDuration(duration);
			header = MimeUtils.getHeader(ServerHeader.REQUEST_RECEIVED.getName(), request.getContent().getHeaders());
			if (header != null) {
				try {
					event.setStarted(HTTPUtils.parseDate(header.getValue()));
					event.setStopped(new Date());
				}
				catch (ParseException e) {
					// couldn't parse date...
				}
			}
			application.getRepository().getComplexEventDispatcher().fire(event, application);
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static HTTPResponse scanForVirus(Artifact from, WebApplication application, Device device, Token token, HTTPRequest request, ComplexContent content) throws ServiceException {
		if (application.getConfig().getVirusScanner() != null) {
			for (Element<?> child : TypeUtils.getAllChildren(content.getType())) {
				Object object = content.get(child.getName());
				if (object != null) {
					if (child.getType().isList(child.getProperties())) {
						CollectionHandlerProvider handler = CollectionHandlerFactory.getInstance().getHandler().getHandler(object.getClass());
						for (Object single : handler.getAsIterable(object)) {
							if (child.getType() instanceof ComplexType) {
								if (!(single instanceof ComplexContent)) {
									single = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(single);
								}
								if (single != null) {
									HTTPResponse response = scanForVirus(from, application, device, token, request, (ComplexContent) single);
									if (response != null) {
										return response;
									}
								}
							}
							// NOTE: we don't scan streams (yet) as they are standard read only
							// to scan them, we would need to simultaneously copy them, either to memory (not ideal for big data) or file (not ideal for small data)
							// there are plenty of solutions in nabu to deal with that, but currently not (yet) necessary as this focuses on web input, which is always byte[] for base64 stuff
							else if (single instanceof byte[]) {
								HTTPResponse response = scanForVirus(from, application, device, token, request, (byte[]) single);
								if (response != null) {
									return response;
								}
							}
						}
					}
					else if (object instanceof byte[]) {
						HTTPResponse response = scanForVirus(from, application, device, token, request, (byte[]) object);
						if (response != null) {
							return response;
						}
					}
					else if (child.getType() instanceof ComplexType) {
						if (!(object instanceof ComplexContent)) {
							object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
						}
						// if it _is_ null at this point, it could not be wrapped...we ignore this for now...
						if (object != null) {
							HTTPResponse response = scanForVirus(from, application, device, token, request, (ComplexContent) object);
							if (response != null) {
								return response;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public static HTTPResponse scanForVirus(Artifact from, WebApplication application, Device device, Token token, HTTPRequest request) throws ServiceException {
		return scanForVirus(from, application, device, token, request, (byte[]) null);
	}
	
	public static HTTPResponse scanForVirus(Artifact from, WebApplication application, Device device, Token token, HTTPRequest request, byte [] content) throws ServiceException {
		VirusScanner scanner = application.getConfig().getVirusScanner();
		if (scanner != null) {
			VirusInfection scan = content != null ? scanner.scan(new ByteArrayInputStream(content)) : scanner.scan(request);
			// if we found a virus, we don't allow the request!
			if (scan != null) {
				HTTPComplexEventImpl event = new HTTPComplexEventImpl();
				if (device != null) {
					event.setDeviceId(device.getDeviceId());
				}
				if (token != null) {
					event.setAlias(token.getName());
					event.setRealm(token.getRealm());
				}
				Pipeline pipeline = PipelineUtils.getPipeline();
				CEPUtils.enrich(event, from.getClass(), "virus-detected", pipeline == null || pipeline.getSourceContext() == null ? null : pipeline.getSourceContext().getSocketAddress(), "Virus detected: " + scan.getThreat(), null);
				event.setSeverity(EventSeverity.ERROR);
				event.setMethod(request.getMethod());
				HTTPServerArtifact server = application.getConfig().getVirtualHost().getConfig().getServer();
				try {
					event.setRequestUri(HTTPUtils.getURI(request, server.isSecure()));
				}
				catch (FormatException e) {
					// could not set the request uri... :(
				}
				event.setApplicationProtocol(server.isSecure() ? "HTTPS" : "HTTP");
				event.setArtifactId(server.getId());
				event.setDestinationPort(server.getConfig().getPort());
				Header header = MimeUtils.getHeader("User-Agent", request.getContent().getHeaders());
				if (header != null) {
					event.setUserAgent(MimeUtils.getFullHeaderValue(header));
				}
				header = MimeUtils.getHeader(ServerHeader.REQUEST_RECEIVED.getName(), request.getContent().getHeaders());
				if (header != null) {
					try {
						event.setStarted(HTTPUtils.parseDate(header.getValue()));
						event.setStopped(new Date());
					}
					catch (ParseException e) {
						// couldn't parse date...
					}
				}
				// inject the correct values based on the headers
				event.setSourceIp(HTTPUtils.getRemoteAddress(server.getConfig().isProxied(), request.getContent().getHeaders()));
				event.setSourceHost(HTTPUtils.getRemoteHost(server.getConfig().isProxied(), request.getContent().getHeaders()));
				event.setSizeIn(MimeUtils.getContentLength(request.getContent().getHeaders()));
				// the virus code
				event.setCode(scan.getThreat());
				event.setArtifactId(from.getId());
				event.setContext(application.getId());
				application.getRepository().getComplexEventDispatcher().fire(event, application);
				// we send back a 403
				return new DefaultHTTPResponse(request, 403, HTTPCodes.getMessage(403), new PlainMimeEmptyPart(null, new MimeHeader("Content-Length", "0")));
			}
		}
		return null;
	}
}
