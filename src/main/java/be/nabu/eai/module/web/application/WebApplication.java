package be.nabu.eai.module.web.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.Key;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.authentication.api.PasswordAuthenticator;
import be.nabu.eai.authentication.api.SecretAuthenticator;
import be.nabu.eai.module.http.server.RepositoryExceptionFormatter;
import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.module.web.application.WebConfiguration.WebConfigurationPart;
import be.nabu.eai.module.web.application.api.RequestSubscriber;
import be.nabu.eai.module.web.application.rate.RateLimiter;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.MetricsLevelProvider;
import be.nabu.eai.repository.api.AuthenticatorProvider;
import be.nabu.eai.repository.api.CacheProviderArtifact;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.LicenseManager;
import be.nabu.eai.repository.api.LicensedRepository;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.Translator;
import be.nabu.eai.repository.api.UserLanguageProvider;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.impl.CacheSessionProvider;
import be.nabu.eai.repository.util.CombinedAuthenticator;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.api.StringSubstituter;
import be.nabu.glue.api.StringSubstituterProvider;
import be.nabu.glue.core.impl.methods.v2.HashMethods;
import be.nabu.glue.core.impl.parsers.GlueParserProvider;
import be.nabu.glue.core.repositories.ScannableScriptRepository;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.glue.utils.MultipleRepository;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.glue.utils.ScriptUtils;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.DeviceValidator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.authentication.impl.TimeoutTokenValidator;
import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.cache.api.CacheProvider;
import be.nabu.libs.cache.impl.AccessBasedTimeoutManager;
import be.nabu.libs.cache.impl.LastModifiedTimeoutManager;
import be.nabu.libs.cache.impl.SerializableSerializer;
import be.nabu.libs.cache.impl.StringSerializer;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.events.filters.AndEventFilter;
import be.nabu.libs.http.api.ContentRewriter;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.api.server.SessionProvider;
import be.nabu.libs.http.api.server.SessionResolver;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.GluePostProcessListener;
import be.nabu.libs.http.glue.GluePreprocessListener;
import be.nabu.libs.http.glue.GlueSessionResolver;
import be.nabu.libs.http.glue.GlueWebParserProvider;
import be.nabu.libs.http.glue.HTTPResponseDataSerializer;
import be.nabu.libs.http.glue.api.CacheKeyProvider;
import be.nabu.libs.http.glue.impl.RequestMethods;
import be.nabu.libs.http.glue.impl.UserMethods;
import be.nabu.libs.http.jwt.JWTSessionProvider;
import be.nabu.libs.http.jwt.impl.JWTBearerHandler;
import be.nabu.libs.http.server.BasicAuthenticationHandler;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.http.server.ResourceHandler;
import be.nabu.libs.http.server.SessionProviderImpl;
import be.nabu.libs.metrics.api.GroupLevelProvider;
import be.nabu.libs.metrics.api.MetricInstance;
import be.nabu.libs.metrics.core.api.SinkEvent;
import be.nabu.libs.metrics.core.api.SinkProvider;
import be.nabu.libs.metrics.core.filters.ThresholdOverTimeFilter;
import be.nabu.libs.metrics.core.sinks.LimitedHistorySinkProvider;
import be.nabu.libs.metrics.impl.MetricGrouper;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.SourceContext;
import be.nabu.libs.resources.CombinedContainer;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.TimestampedResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.fixed.FixedInputService;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.TypeBaseUtils;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;

/**
 * TODO: integrate session provider to use same cache as service cache
 * Maybe re-add "AnyThreadTracker" to the context?
 * 
 * TODO: the configuration for the webfragments is absolutely necessary but it does not work well with the paths
 * The only reason the paths were added was so you could mount the same fragment twice and configure it differently (e.g. two wiki endpoints)
 * However with the method extension logic introduced later on, the paths lose relevance quickly
 * An alternative approach could be to remove paths alltogether and match everything on type
 * We could still support the (very rare) occassion when you want to mount something twice by allowing webcomponent-level configuration
 * This would require you to add the fragment the second time to a web component, configure it there and add the web component to the application
 */
public class WebApplication extends JAXBArtifact<WebApplicationConfiguration> implements StartableArtifact, StoppableArtifact, AuthenticatorProvider, WebFragmentProvider {

	public static final String MODULE = "nabu.web.application";
	private Map<ResourceContainer<?>, ScriptRepository> additionalRepositories = new HashMap<ResourceContainer<?>, ScriptRepository>();
	private List<EventSubscription<?, ?>> subscriptions = new ArrayList<EventSubscription<?, ?>>();
	private GlueListener listener;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private SessionProvider sessionProvider;
	private boolean started;
	private MultipleRepository repository;
	private ServiceMethodProvider serviceMethodProvider;
	private ResourceHandler resourceHandler;
	private WebConfiguration fragmentConfiguration;
	private CombinedAuthenticator authenticator;
	private RoleHandler roleHandler;
	
	private boolean authenticatorResolved, roleHandlerResolved, permissionHandlerResolved, tokenValidatorResolved, languageProviderResolved, deviceValidatorResolved;
	private PermissionHandler permissionHandler;
	private TokenValidator tokenValidator;
	private UserLanguageProvider userLanguageProvider;
	private DeviceValidator deviceValidator;
	
	// typeId > regex > content
	private Map<String, Map<String, ComplexContent>> fragmentConfigurations;
	// very bad solution to keep track of which configuration is environment specific (almost none are)
	private List<ComplexContent> environmentSpecificConfigurations = new ArrayList<ComplexContent>();
	private String version;
	private GlueWebParserProvider parserProvider;
	private boolean rateLimiterResolved;
	private RateLimiter rateLimiter;
	
	public WebApplication(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "webartifact.xml", WebApplicationConfiguration.class);
	}

	@Override
	public void stop() throws IOException {
		started = false;
		sessionProvider = null;
		logger.info("Stopping " + subscriptions.size() + " subscriptions");
		for (EventSubscription<?, ?> subscription : subscriptions) {
			subscription.unsubscribe();
		}
		subscriptions.clear();
		// unregister codes
		HTTPServer server = getConfiguration().getVirtualHost().getConfiguration().getServer().getServer();
		if (server != null && server.getExceptionFormatter() instanceof RepositoryExceptionFormatter) {
			((RepositoryExceptionFormatter) server.getExceptionFormatter()).unregister(getId());
		}
		if (getConfiguration().getSessionCacheProvider() != null) {
			getConfiguration().getSessionCacheProvider().remove(getId() + "-session");
		}
		List<WebFragment> webFragments = getConfiguration().getWebFragments();
		if (webFragments != null) {
			for (WebFragment fragment : webFragments) {
				if (fragment != null) {
					fragment.stop(this, null);
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void start() throws IOException {
		boolean licensed = true;
		if (getRepository() instanceof LicensedRepository) {
			LicenseManager licenseManager = ((LicensedRepository) getRepository()).getLicenseManager();
			licensed = licenseManager != null && licenseManager.isLicensed(MODULE);
			if (!licensed) {
				logger.warn("No license found for the web application module, script caches are disabled");
			}
		}
		boolean isDevelopment = EAIResourceRepository.isDevelopment();
		if (!started && getConfiguration().getVirtualHost() != null) {
			buildConfiguration();
			
			// load the rest
			String realm = getRealm();
			String serverPath = getServerPath();

			ResourceContainer<?> publicDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PUBLIC);
			ResourceContainer<?> privateDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PRIVATE);
			
			serviceMethodProvider = new ServiceMethodProvider(getRepository(), getRepository());
			
			ResourceContainer<?> meta = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("meta");
			ScriptRepository metaRepository = null;
			if (meta != null) {
				metaRepository = new ScannableScriptRepository(null, meta, new GlueParserProvider(serviceMethodProvider), Charset.defaultCharset());
			}
			
			repository = new MultipleRepository(null);
			List<ContentRewriter> rewriters = new ArrayList<ContentRewriter>();
			
			// add whitelisted codes to the server exception formatter
			if (getConfiguration().getVirtualHost().getConfiguration().getServer() != null) {
				HTTPServer server = getConfiguration().getVirtualHost().getConfiguration().getServer().getServer();
				if (server.getExceptionFormatter() instanceof RepositoryExceptionFormatter && getConfiguration().getWhitelistedCodes() != null) {
					((RepositoryExceptionFormatter) server.getExceptionFormatter()).register(getId(), Arrays.asList(getConfiguration().getWhitelistedCodes().split("[\\s]*,[\\s]*")));
				}
			}
			
			// TODO: if we have a jwt token, register a listener early on that can spot a Authorization: Bearer <token> header
			// and transform it into a security header

			// create session provider
			if (licensed && getConfiguration().getSessionCacheProvider() != null) {
				Cache sessionCache = getConfiguration().getSessionCacheProvider().create(
					getConfig().getSessionCacheId() == null ? getId() + "-session" : getConfig().getSessionCacheId(),
					// defaults to unlimited
					getConfiguration().getMaxTotalSessionSize() == null ? 0 : getConfiguration().getMaxTotalSessionSize(),
					// defaults to unlimited
					getConfiguration().getMaxSessionSize() == null ? 0 : getConfiguration().getMaxSessionSize(),
					new StringSerializer(),
					new SerializableSerializer(),
					// no refreshing logic obviously
					null,
					// defaults to 1 hour
					new AccessBasedTimeoutManager(getConfiguration().getSessionTimeout() == null ? 1000l*60*60 : getConfiguration().getSessionTimeout())
				);
				sessionProvider = new CacheSessionProvider(sessionCache);
			}
			else {
				sessionProvider = new SessionProviderImpl(getConfiguration().getSessionTimeout() == null ? 1000l*60*60 : getConfiguration().getSessionTimeout());
			}

			if (licensed && getConfig().getJwtKeyAlias() != null && getConfig().getJwtKeyStore() != null) {
				KeyStoreArtifact keystore = getConfig().getJwtKeyStore();
				try {
					sessionProvider = new JWTSessionProvider(
						GlueListener.buildTokenName(getRealm()),
						sessionProvider,
						keystore.getKeyStore().getSecretKey(getConfig().getJwtKeyAlias()));
				}
				catch (Exception e) {
					logger.info("JWT key alias '" + getConfig().getJwtKeyAlias() + " is not a secret key, skipping JWT session management");
				}
			}
			
			Map<String, String> environment = getProperties();
			if (isDevelopment) {
				environment.put("development", "true");
			}
			// always set the id of the web artifact (need it to introspect artifact)
			String hostName = getConfiguration().getVirtualHost().getConfiguration().getHost();
			Integer port = getConfiguration().getVirtualHost().getConfiguration().getServer().getConfiguration().getPort();
			boolean secure = getConfiguration().getVirtualHost().getConfiguration().getServer().getConfiguration().getKeystore() != null;

			String host = null;
			if (hostName != null) {
				if (port != null) {
					hostName += ":" + port;
				}
				host = secure ? "https://" : "http://";
				host += hostName;
			}
			
			environment.put("mobile", "false");
			environment.put("web", "true");
			environment.put("webApplicationId", getId());
			environment.put("secure", Boolean.toString(secure));
			environment.put("url", host);
			environment.put("host", hostName);
			environment.put("hostName", getConfiguration().getVirtualHost().getConfiguration().getHost());
			environment.put("version", getVersion());
			
			String environmentName = serverPath;
			if (environmentName.startsWith("/")) {
				environmentName.substring(1);
			}
			if (environmentName.isEmpty()) {
				environmentName = "root";
			}
			
			EventDispatcher dispatcher = getConfiguration().getVirtualHost().getDispatcher();
			
			// allow custom request handlers, request rewriting & response rewriting are the domain of the glue pre & post processors
			if (getConfiguration().getRequestSubscriber() != null) {
				final RequestSubscriber requestSubscriber = POJOUtils.newProxy(RequestSubscriber.class, wrap(getConfiguration().getRequestSubscriber(), getMethod(RequestSubscriber.class, "handle")), getRepository(), SystemPrincipal.ROOT);
				EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
					@Override
					public HTTPResponse handle(HTTPRequest event) {
						SourceContext sourceContext = PipelineUtils.getPipeline().getSourceContext();
						return requestSubscriber.handle(getId(), getRealm(), new SourceImpl(sourceContext), event);
					}
				});
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
			}
			
			// before the base authentication required authenticate header rewriter, add a rewriter for the response (if applicable)
			if (metaRepository != null) {
				GluePostProcessListener postprocessListener = new GluePostProcessListener(
					metaRepository, 
					new SimpleExecutionEnvironment(environmentName, environment),
					serverPath
				);
				postprocessListener.setRefresh(isDevelopment);
				postprocessListener.setRealm(realm);
				EventSubscription<HTTPResponse, HTTPResponse> subscription = dispatcher.subscribe(HTTPResponse.class, postprocessListener);
				subscription.filter(HTTPServerUtils.limitToRequestPath(serverPath));
				subscriptions.add(subscription);
			}
			
			// ie caches everything by default, including the responses from rest services
			// this post processor will by default add no cache headers unless disabled
			if (getConfig().isAddCacheHeaders()) {
				EventSubscription<HTTPResponse, HTTPResponse> subscription = dispatcher.subscribe(HTTPResponse.class, new EventHandler<HTTPResponse, HTTPResponse>() {
					@Override
					public HTTPResponse handle(HTTPResponse event) {
						if (event.getContent() == null) {
							return null;
						}
						Header lastModified = MimeUtils.getHeader("Last-Modified", event.getContent().getHeaders());
						if (lastModified == null) {
							Header cacheControl = MimeUtils.getHeader("Cache-Control", event.getContent().getHeaders());
							if (cacheControl == null) {
								String contentType = MimeUtils.getContentType(event.getContent().getHeaders());
								if (contentType != null && contentType.matches("application/(json|xml)")) {
									event.getContent().setHeader(
										new MimeHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0"),
										new MimeHeader("Pragma", "no-cache")
									);
								}
							}
						}
						return null;
					}
				});
				subscription.filter(HTTPServerUtils.limitToRequestPath(serverPath));
				subscriptions.add(subscription);
			}
			
			// set up a basic authentication listener which optionally interprets that, it allows for REST-based access
			Authenticator authenticator = getAuthenticator();
			if (getConfiguration().getAllowBasicAuthentication() != null && getConfiguration().getAllowBasicAuthentication()) {
				BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler(authenticator, HTTPServerUtils.newFixedRealmHandler(realm));
				// make sure it is not mandatory
				basicAuthenticationHandler.setRequired(false);
				EventSubscription<HTTPRequest, HTTPResponse> authenticationSubscription = dispatcher.subscribe(HTTPRequest.class, basicAuthenticationHandler);
				authenticationSubscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(authenticationSubscription);
				
				// for all responses, we check a 401 to see if it has the required WWW-Authenticate header
				// in retrospect: don't add it? (maybe configurable?)
				// problem is it pops up a window in the browser to authenticate
//				EventSubscription<HTTPResponse, HTTPResponse> ensureAuthenticationSubscription = dispatcher.subscribe(HTTPResponse.class, HTTPServerUtils.ensureAuthenticateHeader(realm));
//				ensureAuthenticationSubscription.filter(HTTPServerUtils.limitToRequestPath(serverPath));
//				subscriptions.add(ensureAuthenticationSubscription);
			}
			
			if (licensed && getConfig().isAllowJwtBearer() && getConfig().getJwtKeyAlias() != null && getConfig().getJwtKeyStore() != null) {
				KeyStoreArtifact keystore = getConfig().getJwtKeyStore();
				Key key = null;
				try {
					key = keystore.getKeyStore().getCertificate(getConfig().getJwtKeyAlias()).getPublicKey();
				}
				catch (Exception e) {
					try {
						key = keystore.getKeyStore().getChain(getConfig().getJwtKeyAlias())[0].getPublicKey();
					}
					catch (Exception f) {
						logger.info("JWT key alias '" + getConfig().getJwtKeyAlias() + " does not have a certificate, skipping JWT bearer tokens");
					}
				}
				if (key != null) {
					EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, new JWTBearerHandler(key, getRealm()));
					subscription.filter(HTTPServerUtils.limitToPath(serverPath));
					subscriptions.add(subscription);
				}
			}
			
			// after the base authentication but before anything else, allow for rewriting
			if (metaRepository != null) {
				GluePreprocessListener preprocessListener = new GluePreprocessListener(
					authenticator,
					sessionProvider, 
					metaRepository, 
					new SimpleExecutionEnvironment(environmentName, environment),
					serverPath
				);
				preprocessListener.setRefresh(isDevelopment);
				preprocessListener.setTokenValidator(getTokenValidator());
				preprocessListener.setRealm(realm);
				EventSubscription<HTTPRequest, HTTPRequest> subscription = dispatcher.subscribe(HTTPRequest.class, preprocessListener);
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(subscription);
			}
			
			parserProvider = new GlueWebParserProvider(serviceMethodProvider);
			ResourceContainer<?> resources = null;
			if (publicDirectory != null) {
				// check if there is a resource directory
				resources = (ResourceContainer<?>) publicDirectory.getChild("resources");
				if (resources != null) {
					logger.debug("Adding resource listener for folder: " + resources);
					if (isDevelopment && privateDirectory != null) {
						Resource child = privateDirectory.getChild("resources");
						if (child != null) {
							resources = new CombinedContainer(null, "resources", resources, (ResourceContainer<?>) child);
						}
					}
					// add optimizations if it is not development
					if (!isDevelopment) {
//						logger.debug("Adding javascript merger");
//						JavascriptMerger javascriptMerger = new JavascriptMerger(resources, resourcePath);
//						EventSubscription<HTTPRequest, HTTPResponse> javascriptMergerSubscription = server.getEventDispatcher().subscribe(HTTPRequest.class, javascriptMerger);
//						subscriptions.add(javascriptMergerSubscription);
//						javascriptMergerSubscription.filter(HTTPServerUtils.limitToPath(resourcePath));
//						rewriters.add(javascriptMerger);
//						
//						logger.debug("Adding css merger");
//						CSSMerger cssMerger = new CSSMerger(resources, resourcePath);
//						EventSubscription<HTTPRequest, HTTPResponse> cssMergerSubscription = server.getEventDispatcher().subscribe(HTTPRequest.class, cssMerger);
//						subscriptions.add(cssMergerSubscription);
//						cssMergerSubscription.filter(HTTPServerUtils.limitToPath(resourcePath));
//						rewriters.add(cssMerger);
					}
				}
				if (privateDirectory != null) {
					// externally provided resources
					Resource resolve = ResourceUtils.resolve(privateDirectory, "provided/resources");
					if (resolve != null) {
						resources = resources == null ? (ResourceContainer<?>) resolve : new CombinedContainer(null, "resources", resources, (ResourceContainer<?>) resolve);
					}
				}
				ResourceContainer<?> pages = (ResourceContainer<?>) publicDirectory.getChild("pages");
				if (pages != null) {
					logger.debug("Adding public scripts found in: " + pages);
					// the configured charset is for the end user, NOT for the local glue scripts, that should be the system default
					ScannableScriptRepository scannableScriptRepository = new ScannableScriptRepository(repository, pages, parserProvider, Charset.defaultCharset());
					scannableScriptRepository.setGroup(GlueListener.PUBLIC);
					repository.add(scannableScriptRepository);
				}
			}

			String resourcePath = serverPath.equals("/") ? "/resources" : serverPath + "/resources";
			resourceHandler = new ResourceHandler(resources, resourcePath, !isDevelopment);
			EventSubscription<HTTPRequest, HTTPResponse> resourceSubscription = dispatcher.subscribe(HTTPRequest.class, resourceHandler);
			resourceSubscription.filter(HTTPServerUtils.limitToPath(resourcePath));
			subscriptions.add(resourceSubscription);

			// the private directory houses the scripts
			if (privateDirectory != null) {
				// currently only a scripts folder, but we may want to add more private folders later on
				ResourceContainer<?> scripts = (ResourceContainer<?>) privateDirectory.getChild("scripts");
				if (scripts != null) {
					logger.debug("Adding private scripts found in: " + scripts);
					repository.add(new ScannableScriptRepository(repository, scripts, parserProvider, Charset.defaultCharset()));
				}
				// externally provided scripts
				Resource resolve = ResourceUtils.resolve(privateDirectory, "provided/artifacts");
				if (resolve != null) {
					logger.debug("Adding private provided artifacts found in: " + resolve);
					repository.add(new ScannableScriptRepository(repository, (ResourceContainer<?>) resolve, parserProvider, Charset.defaultCharset()));
				}
			}
			listener = new GlueListener(
				sessionProvider, 
				repository, 
				new SimpleExecutionEnvironment(environmentName, environment),
				serverPath
			);
			if (getConfig().getCookiePath() != null) {
				listener.setCookiePath(getConfig().getCookiePath());
			}
			
			final CacheProviderArtifact cacheProvider = getConfiguration().getScriptCacheProvider();
			if (licensed && cacheProvider != null && !isDevelopment) {
				listener.setCacheProvider(new CacheProvider() {
					@Override
					public Cache get(String name) throws IOException {
						Cache cache = cacheProvider.get(getId() + "-script-" + name);
						if (cache == null) {
							synchronized(cacheProvider) {
								cache = cacheProvider.get(getId() + "-script-" + name);
								if (cache == null) {
									Long configuredTimeout = null;
									try {
										Script script = listener.getRepository().getScript(name);
										if (script.getRoot().getContext() != null && script.getRoot().getContext().getAnnotations() != null) {
											String string = script.getRoot().getContext().getAnnotations().get("cache");
											if (string != null && !string.trim().isEmpty()) {
												String[] parts = string.split("[\\s]*,[\\s]*");
												for (String part : parts) {
													if (part.matches("[0-9-]+")) {
														configuredTimeout = Long.parseLong(part);
													}
												}
											}
										}
									}
									catch (ParseException e) {
										logger.warn("Can not check for configured timeout for glue script '" + name + "'", e);
									}
									if (configuredTimeout == null) {
										configuredTimeout = getConfiguration().getScriptCacheTimeout() == null ? 1000l*60*60 : getConfiguration().getScriptCacheTimeout();
									}
									// if you set the timeout to negative, it should not be cached
									if (configuredTimeout < 0) {
										return null;
									}
									cache = cacheProvider.create(getId() + "-script-" + name, 
										// defaults to unlimited
										getConfiguration().getMaxTotalScriptCacheSize() == null ? 0 : getConfiguration().getMaxTotalScriptCacheSize(),
										// defaults to unlimited
										getConfiguration().getMaxScriptCacheSize() == null ? 0 : getConfiguration().getMaxScriptCacheSize(),
										new StringSerializer(Charset.forName("UTF-8")), 
										new HTTPResponseDataSerializer(), 
										null, 
										// defaults to 1 hour
										new LastModifiedTimeoutManager(configuredTimeout)
//										new AccessBasedTimeoutManager(configuredTimeout)
									);
								}
							}
						}
						return cache;
					}
					@Override
					public void remove(String name) throws IOException {
						cacheProvider.remove(getId() + "-script-" + name);
					}
				});
			}
			
			if (getConfiguration().getFailedLoginThreshold() != null) {
				MetricInstance metricInstance = getRepository().getMetricInstance(getId());
				// we need to up the grouping level of the login failed, otherwise we never get to the ip level
				if (metricInstance instanceof MetricGrouper) {
					GroupLevelProvider groupLevelProvider = ((MetricGrouper) metricInstance).getGroupLevelProvider();
					if (groupLevelProvider instanceof MetricsLevelProvider) {
						((MetricsLevelProvider) groupLevelProvider).set(UserMethods.METRICS_LOGIN_FAILED, 2);
					}
					else {
						throw new IllegalArgumentException("Expecting a MetricsLevelProvider");	
					}
				}
				// add an event dispatcher to trigger on failed logins and blacklist an ip
				EventDispatcher metricsDispatcher = getRepository().getMetricsDispatcher();
				if (metricsDispatcher != null) {
					// by default the user will be blocked for 15 minutes
					EventSubscription<SinkEvent, Void> metricsSubscription = metricsDispatcher.subscribe(SinkEvent.class, new FailedLoginListener(this, getConfiguration().getFailedLoginBlacklistDuration() == null ? 15 * 60000l : getConfiguration().getFailedLoginBlacklistDuration()));
					metricsSubscription.filter(new AndEventFilter<SinkEvent>(
						// we are only interested in login metrics for this artifact
						new EventHandler<SinkEvent, Boolean>() {
							@Override
							public Boolean handle(SinkEvent event) {
								boolean isAllowed = getId().equals(event.getId()) && !event.getCategory().startsWith(UserMethods.METRICS_LOGIN_FAILED + ":");
								// filter all those that are not allowed
								return !isAllowed;
							}
						},
						// the default window is 10 minutes
						new ThresholdOverTimeFilter(
							getConfiguration().getFailedLoginThreshold(), 
							true, 
							getConfiguration().getFailedLoginWindow() == null ? 600000l : getConfiguration().getFailedLoginWindow()
						)
					));
					subscriptions.add(metricsSubscription);
				}
				listener.setMetrics(metricInstance);
			}
			
			final UserLanguageProvider languageProvider = getLanguageProvider();
			if (getConfiguration().getTranslationService() != null) {
				final Map<String, ComplexContent> translatorValues = getInputValues(getConfig().getTranslationService(), getMethod(Translator.class, "translate"));
				final String additional;
				final String key;
				// the translator is a special case (compared with the other services) because it is not used as an object, so we can't wrap it
				// instead the id of the service is injected into glue to be called
				// currently the way to solve it is to detect these "additional" parameters, json stringify them and inject them as json
				// classic type masking in glue will make sure the unmarshalled version is "transformed" to the correct type
				if (translatorValues != null && translatorValues.size() > 0) {
					if (translatorValues.size() > 1) {
						throw new RuntimeException("Translation services can only have one extended field");
					}
					key = translatorValues.keySet().iterator().next();
					JSONBinding binding = new JSONBinding(translatorValues.get(key).getType());
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					binding.marshal(output, translatorValues.get(key));
					additional = new String(output.toByteArray(), Charset.forName("UTF-8")).replace("'", "\\'");
				}
				else {
					additional = null;
					key = null;
				}
				listener.getSubstituterProviders().add(new StringSubstituterProvider() {
					@Override
					public StringSubstituter getSubstituter(ScriptRuntime runtime) {
						String language = null;
						// if we have a language provider, try to get it from there
						if (languageProvider != null) {
							Token token = UserMethods.token();
							// the token may be null then the provider can send back a default language
							language = languageProvider.getLanguage(token);
						}
						// if there is no language provider or it does not have a language for the user, try to detect from browser settings
						if (language == null) {
							if (RequestMethods.content().getContent() != null) {
								List<String> acceptedLanguages = MimeUtils.getAcceptedLanguages(RequestMethods.content().getContent().getHeaders());
								if (!acceptedLanguages.isEmpty()) {
									language = acceptedLanguages.get(0).replaceAll("-.*$", "");
								}
							}
						}
						try {
							if (additional != null) {
								return new be.nabu.glue.impl.ImperativeSubstitutor("%", "template(" + getConfiguration().getTranslationService().getId() 
										+ "(when(\"${value}\" ~ \"^[a-z0-9.]+:.*\", replace(\"^([a-z0-9.]+):.*\", \"$1\", \"${value}\"), \"page:" + ScriptUtils.getFullName(runtime.getScript()) + "\"), "
										+ "when(\"${value}\" ~ \"^[a-z0-9.]+:.*\", replace(\"^[a-z0-9.]+:(.*)\", \"$1\", \"${value}\"), \"${value}\"), " + (language == null ? "null" : "\"" + language + "\"")
										+ ", " + key + ": json.objectify('" + additional + "'))/translation)");
							}
							else {
								return new be.nabu.glue.impl.ImperativeSubstitutor("%", "template(" + getConfiguration().getTranslationService().getId() 
										+ "(when(\"${value}\" ~ \"^[a-z0-9.]+:.*\", replace(\"^([a-z0-9.]+):.*\", \"$1\", \"${value}\"), \"page:" + ScriptUtils.getFullName(runtime.getScript()) + "\"), "
										+ "when(\"${value}\" ~ \"^[a-z0-9.]+:.*\", replace(\"^[a-z0-9.]+:(.*)\", \"$1\", \"${value}\"), \"${value}\"), " + (language == null ? "null" : "\"" + language + "\"") + ")/translation)");
							}
						}
						catch (IOException e) {
							throw new RuntimeException("Could not get translation service", e);
						}
					}
				});
			}
			// set an additional cache key provider that can choose the user language
			if (languageProvider != null) {
				listener.addCacheKeyProvider(new CacheKeyProvider() {
					@Override
					public String getAdditionalCacheKey(HTTPRequest request, Token token, Script script) {
						// make sure we mirror the logic above for the substitution cause that will be the actual language in the returned content
						String language = languageProvider.getLanguage(token);
						if (language == null) {
							List<String> acceptedLanguages = MimeUtils.getAcceptedLanguages(request.getContent().getHeaders());
							if (!acceptedLanguages.isEmpty()) {
								language = acceptedLanguages.get(0).replaceAll("-.*$", "");
							}
						}
						return language;
					}
				});
			}
			
			listener.getContentRewriters().addAll(rewriters);
			listener.setRefreshScripts(isDevelopment);
			listener.setAllowEncoding(!isDevelopment);
			listener.setAuthenticator(authenticator);
			listener.setTokenValidator(getTokenValidator());
			listener.setPermissionHandler(getPermissionHandler());
			listener.setRoleHandler(getRoleHandler());
			listener.setDeviceValidator(getDeviceValidator());
			listener.setRealm(realm);
			// always creating a session can create other issues
//			listener.setAlwaysCreateSession(true);
			List<WebFragment> webFragments = getConfiguration().getWebFragments();
			// new list so we don't affect the original order
			webFragments = webFragments == null ? new ArrayList<WebFragment>() : new ArrayList<WebFragment>(webFragments);
			Collections.sort(webFragments, new Comparator<WebFragment>() {
				@Override
				public int compare(WebFragment o1, WebFragment o2) {
					if (o1 == null) {
						return 1;
					}
					else if (o2 == null) {
						return -1;
					}
					else {
						return o1.getPriority().compareTo(o2.getPriority());
					}
				}
			});
			// first start everything above normal priority
			for (WebFragment fragment : webFragments) {
				if (fragment != null) {
					if (fragment.getPriority().compareTo(WebFragmentPriority.NORMAL) < 0) {
						fragment.start(this, null);
					}
					else {
						break;
					}
				}
			}
			// start the glue listener
			EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, listener);
			subscription.filter(HTTPServerUtils.limitToPath(serverPath));
			subscriptions.add(subscription);
			// start remaining fragments
			for (WebFragment fragment : webFragments) {
				if (fragment != null && fragment.getPriority().compareTo(WebFragmentPriority.NORMAL) >= 0) {
					fragment.start(this, null);
				}
			}
			started = true;
			logger.info("Started " + subscriptions.size() + " subscriptions");
		}
	}

//	private void subscribeToRepositoryChanges(ScriptRepository repository) {
//		subscriptions.add(getRepository().getEventDispatcher().subscribe(NodeEvent.class, new EventHandler<NodeEvent, Void>() {
//			@Override
//			public Void handle(NodeEvent event) {
//				// when a service is loaded or reloaded, we need to check which scripts use it and refresh those
//				if (event.getState() == State.LOAD || event.getState() == State.RELOAD) {
//					for (Script script : ScriptUtils.getRoot(repository)) {
//						Set<String> references = new HashSet<String>();
//						try {
//							GlueServiceManager.getReferences(script.getRoot(), references);
//							if (references.contains(event.getId())) {
////								script.f
//							}
//						}
//						catch (Exception e) {
//							logger.error("Could not check if script '" + ScriptUtils.getFullName(script) + "' needs to be reloaded", e);
//						}
//					}
//				}
//				return null;
//			}
//		}));
//	}

	public String getVersion() throws IOException {
		if (this.version == null) {
			synchronized(this) {
				if (this.version == null) {
					String version = "initial";
					Entry entry = getRepository().getEntry(getId());
					// build an automatic version
					if (entry != null) {
						Node node = entry.getNode();
						String nodeModified = "";
						// include the last modified of the actual node.xml file, on deployment this changes even if nothing else was changed
						if (entry instanceof ResourceEntry) {
							Resource child = ((ResourceEntry) entry).getContainer().getChild("node.xml");
							if (child instanceof TimestampedResource) {
								nodeModified = ((TimestampedResource) child).getLastModified().toString();
							}
						}
						version = (String) HashMethods.md5(nodeModified + "." + node.getEnvironmentId() + "." + node.getVersion() + "." + node.getLastModified().toString());
					}
					this.version = version;
				}
			}
		}
		return version;
	}

	private void buildConfiguration() {
		fragmentConfigurations = new HashMap<String, Map<String,ComplexContent>>();
		// load the fragment configurations
		WebConfiguration fragmentConfiguration = getFragmentConfiguration();
		if (fragmentConfiguration != null && fragmentConfiguration.getParts() != null) {
			for (WebConfigurationPart part : fragmentConfiguration.getParts()) {
				if (!fragmentConfigurations.containsKey(part.getType())) {
					fragmentConfigurations.put(part.getType(), new HashMap<String, ComplexContent>());
				}
				DefinedType resolve = (DefinedType) getRepository().resolve(part.getType());
				if (resolve == null) {
					logger.error("Can not find type '" + part.getType() + "', skipping");
					continue;
				}
				ComplexContent newInstance = ((ComplexType) resolve).newInstance();
				Set<String> keySet = part.getConfiguration().keySet();
				Iterator<String> iterator = keySet.iterator();
				while(iterator.hasNext()) {
					String key = iterator.next();
					try {
						newInstance.set(key.replace('.', '/'), part.getConfiguration().get(key));
					}
					catch (Exception e) {
						logger.warn("Removing " + part.getPathRegex() + ", " + key + " = " + part.getConfiguration().get(key), e);
						iterator.remove();
					}
				}
				fragmentConfigurations.get(part.getType()).put(part.getPathRegex(), newInstance);
				if (part.getEnvironmentSpecific() != null && part.getEnvironmentSpecific()) {
					environmentSpecificConfigurations.add(newInstance);
				}
			}
		}
	}
	
	public Map<String, String> getProperties() throws IOException {
		// load properties
		Properties properties = new Properties();
		if (getDirectory().getChild(".properties") instanceof ReadableResource) {
			logger.debug("Adding properties found in: " + getDirectory().getChild(".properties"));
			InputStream input = IOUtils.toInputStream(new ResourceReadableContainer((ReadableResource) getDirectory().getChild(".properties")));
			try {
				properties.load(input);
			}
			finally {
				input.close();
			}
		}
		Map<String, String> environment = new HashMap<String, String>();
		if (!properties.isEmpty()) {
			for (Object key : properties.keySet()) {
				if (key == null) {
					continue;
				}
				environment.put(key.toString().trim(), properties.getProperty(key.toString()).trim());
			}
		}
		return environment;
	}

	@Override
	public String getRealm() {
		try {
			return getConfiguration().getRealm() == null ? getId() : getConfiguration().getRealm();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getServerPath() throws IOException {
		String serverPath = getConfiguration().getPath();
		if (serverPath == null) {
			serverPath = "/";
		}
		else if (!serverPath.startsWith("/")) {
			serverPath = "/" + serverPath;
		}
		return serverPath;
	}
	
	@Override
	public Authenticator getAuthenticator() {
		if (!authenticatorResolved) {
			synchronized(this) {
				if (!authenticatorResolved) {
					authenticatorResolved = true;
					try {
						PasswordAuthenticator passwordAuthenticator = null;
						if (getConfiguration().getPasswordAuthenticationService() != null) {
							passwordAuthenticator = POJOUtils.newProxy(PasswordAuthenticator.class, wrap(getConfiguration().getPasswordAuthenticationService(), getMethod(PasswordAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
						}
						SecretAuthenticator sharedSecretAuthenticator = null;
						if (getConfiguration().getSecretAuthenticationService() != null) {
							sharedSecretAuthenticator = POJOUtils.newProxy(SecretAuthenticator.class, wrap(getConfiguration().getSecretAuthenticationService(), getMethod(SecretAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
						}
						if (passwordAuthenticator != null || sharedSecretAuthenticator != null) {
							authenticator = new CombinedAuthenticator(passwordAuthenticator, sharedSecretAuthenticator);
						}
					}
					catch(IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return authenticator;
	}
	
	public static Method getMethod(Class<?> clazz, String name) {
		for (Method method : clazz.getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}
		throw new IllegalArgumentException("Can not find method '" + name + "' in class: " + clazz);
	}
	
	public Service wrap(Service service, Method method) throws IOException {
		Map<String, ComplexContent> inputs = getInputValues(service, method);
		if (inputs.isEmpty()) {
			return service;
		}
		else {
			FixedInputService fixed = new FixedInputService(service);
			for (String name : inputs.keySet()) {
				fixed.setInput(name, inputs.get(name));
			}
			return fixed;
		}
	}

	private Map<String, ComplexContent> getInputValues(Service service, Method method) throws IOException {
		List<Element<?>> inputExtensions = EAIRepositoryUtils.getInputExtensions(service, method);
		Map<String, ComplexContent> inputs = new HashMap<String, ComplexContent>();
		for (Element<?> inputExtension : inputExtensions) {
			if (inputExtension.getType() instanceof ComplexType && inputExtension.getType() instanceof DefinedType) {
				for (WebConfigurationPart fragmentConfiguration : getFragmentConfiguration().getParts()) {
					if (((DefinedType) inputExtension.getType()).getId().equals(fragmentConfiguration.getType())) {
						ComplexContent instance = ((ComplexType) inputExtension.getType()).newInstance();
						for (String key : fragmentConfiguration.getConfiguration().keySet()) {
							instance.set(key, fragmentConfiguration.getConfiguration().get(key));
						}
						inputs.put(inputExtension.getName(), instance);
					}
				}
			}
		}
		return inputs;
	}
	
	public DeviceValidator getDeviceValidator() throws IOException {
		if (!deviceValidatorResolved) {
			synchronized(this) {
				if (!deviceValidatorResolved) {
					deviceValidatorResolved = true;
					deviceValidator = getConfiguration().getDeviceValidatorService() != null 
						? POJOUtils.newProxy(
							DeviceValidator.class, 
							wrap(getConfiguration().getDeviceValidatorService(), getMethod(DeviceValidator.class, "isAllowed")), 
							getRepository(), 
							SystemPrincipal.ROOT
						) : null;
				}
			}
		}
		return deviceValidator;
	}
	
	public RoleHandler getRoleHandler() throws IOException {
		if (!roleHandlerResolved) {
			synchronized(this) {
				if (!roleHandlerResolved) {
					roleHandlerResolved = true;
					if (getConfiguration().getRoleService() != null) {
						roleHandler = POJOUtils.newProxy(RoleHandler.class, wrap(getConfiguration().getRoleService(), getMethod(RoleHandler.class, "hasRole")), getRepository(), SystemPrincipal.ROOT);
					}
				}
			}
		}
		return roleHandler;
	}
	
	public PermissionHandler getPermissionHandler() throws IOException {
		if (!permissionHandlerResolved) {
			synchronized(this) {
				if (!permissionHandlerResolved) {
					permissionHandlerResolved = true;
					if (getConfiguration().getPermissionService() != null) {
						permissionHandler = POJOUtils.newProxy(PermissionHandler.class, wrap(getConfiguration().getPermissionService(), getMethod(PermissionHandler.class, "hasPermission")), getRepository(), SystemPrincipal.ROOT);
					}
				}
			}
		}
		return permissionHandler;
	}
	
	public RateLimiter getRateLimiter() throws IOException {
		if (!rateLimiterResolved) {
			synchronized(this) {
				if (!rateLimiterResolved) {
					rateLimiterResolved = true;
					if (getConfiguration().getRateLimiter() != null) {
						SinkProvider sinkProvider = getConfig().getRateLimiterDatabase() == null ? new LimitedHistorySinkProvider(1000) : getConfig().getRateLimiterDatabase();
						rateLimiter = new RateLimiter(
							getRepository(), 
							getConfig().getRateLimiter(), 
							sinkProvider
						);
					}
				}
			}
		}
		return rateLimiter;
	}
	
	public TokenValidator getTokenValidator() throws IOException {
		if (!tokenValidatorResolved) {
			synchronized(this) {
				if (!tokenValidatorResolved) {
					tokenValidatorResolved = true;
					if (getConfiguration().getTokenValidatorService() != null) {
						tokenValidator = POJOUtils.newProxy(TokenValidator.class, wrap(getConfiguration().getTokenValidatorService(), getMethod(TokenValidator.class, "isValid")), getRepository(), SystemPrincipal.ROOT);
					}
				}
			}
		}
		if (tokenValidator == null) {
			tokenValidator = new TimeoutTokenValidator();
		}
		return tokenValidator;
	}
	
	public UserLanguageProvider getLanguageProvider() throws IOException {
		if (!languageProviderResolved) {
			synchronized(this) {
				if (!languageProviderResolved) {
					languageProviderResolved = true;
					if (getConfiguration().getLanguageProviderService() != null) {
						userLanguageProvider = POJOUtils.newProxy(UserLanguageProvider.class, wrap(getConfiguration().getLanguageProviderService(), getMethod(UserLanguageProvider.class, "getLanguage")), getRepository(), SystemPrincipal.ROOT);
					}
				}
			}
		}
		return userLanguageProvider;
	}
	
	public GlueListener getListener() {
		return listener;
	}

	@Override
	public boolean isStarted() {
		return !subscriptions.isEmpty();
	}

	public SessionProvider getSessionProvider() {
		return sessionProvider;
	}

	public SessionResolver getSessionResolver() {
		return new GlueSessionResolver(sessionProvider);
	}

	public EventDispatcher getDispatcher() {
		try {
			return getConfiguration().getVirtualHost() != null ? getConfiguration().getVirtualHost().getDispatcher() : null;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void addGlueScripts(ResourceContainer<?> parent, boolean isPublic) throws IOException {
		if (repository != null) {
			ScannableScriptRepository scannableScriptRepository = new ScannableScriptRepository(repository, parent, parserProvider, Charset.defaultCharset());
			if (isPublic) {
				scannableScriptRepository.setGroup(GlueListener.PUBLIC);
			}
			additionalRepositories.put(parent, scannableScriptRepository);
			repository.add(scannableScriptRepository);
		}
	}
	public void removeGlueScripts(ResourceContainer<?> parent) {
		if (repository != null && additionalRepositories.containsKey(parent)) {
			repository.remove(additionalRepositories.get(parent));
			additionalRepositories.remove(parent);
		}
	}
	
	public void addResources(ResourceContainer<?> parent) {
		if (resourceHandler != null) {
			resourceHandler.addRoot(parent);
		}
	}
	public void removeResources(ResourceContainer<?> parent) {
		if (resourceHandler != null) {
			resourceHandler.removeRoot(parent);
		}
	}
	
	public ResourceHandler getResourceHandler() {
		return resourceHandler;
	}

	void setFragmentConfiguration(WebConfiguration configuration) {
		fragmentConfiguration = configuration;
	}
	
	WebConfiguration getFragmentConfiguration() {
		if (fragmentConfiguration == null) {
			synchronized(this) {
				if (fragmentConfiguration == null) {
					try {
						Resource child = getDirectory().getChild("fragments.xml");
						if (child != null) {
							ReadableContainer<ByteBuffer> readable = ((ReadableResource) child).getReadable();
							try {
								fragmentConfiguration = WebConfiguration.unmarshal(IOUtils.toInputStream(readable));
							}
							finally {
								readable.close();
							}
						}
						else {
							fragmentConfiguration = new WebConfiguration();
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return fragmentConfiguration;
	}
	
	public ComplexContent getConfigurationFor(String path, ComplexType type) throws IOException {
		return getConfigurationFor(path, type, fragmentConfigurations);
	}

	ComplexContent getConfigurationFor(String path, ComplexType type, Map<String, Map<String, ComplexContent>> fragmentConfigurations) {
		String typeId = ((DefinedType) type).getId();
		Map<String, ComplexContent> map = fragmentConfigurations.get(typeId);
		if (map == null) {
			return null;
		}
		String closestRegex = null;
		ComplexContent closest = null;
		for (String key : map.keySet()) {
			if (key == null || path.matches(key)) {
				if (closestRegex == null || (key != null && key.length() > closestRegex.length())) {
					closest = map.get(key);
				}
			}
		}
		return closest;
	}
	
	Map<String, Map<String, ComplexContent>> getFragmentConfigurations() {
		if (fragmentConfigurations == null) {
			synchronized(this) {
				if (fragmentConfigurations == null) {
					buildConfiguration();
				}
			}
		}
		return fragmentConfigurations;
	}
	
	List<ComplexContent> getEnvironmentSpecificConfigurations() {
		return environmentSpecificConfigurations;
	}

	void setEnvironmentSpecificConfigurations(List<ComplexContent> environmentSpecificConfigurations) {
		this.environmentSpecificConfigurations = environmentSpecificConfigurations;
	}

	@Override
	public void save(ResourceContainer<?> directory) throws IOException {
		WebConfiguration configuration = new WebConfiguration();
		for (String typeId : getFragmentConfigurations().keySet()) {
			for (String regex : getFragmentConfigurations().get(typeId).keySet()) {
				ComplexContent content = getFragmentConfigurations().get(typeId).get(regex);
				WebConfigurationPart part = new WebConfigurationPart();
				part.setConfiguration(TypeBaseUtils.toStringMap(content));
				part.setPathRegex(regex);
				part.setType(typeId);
				part.setEnvironmentSpecific(environmentSpecificConfigurations.contains(content));
				configuration.getParts().add(part);
			}
		}
		// save properties
		Resource resource = directory.getChild("fragments.xml");
		if (resource == null) {
			resource = ((ManageableContainer<?>) directory).create("fragments.xml", "application/xml");
		}
		WritableContainer<ByteBuffer> writable = ((WritableResource) resource).getWritable();
		try {
			configuration.marshal(IOUtils.toOutputStream(writable));
		}
		finally {
			writable.close();
		}
		super.save(directory);
	}
	
	public String getCookiePath() {
		if (getConfig().getCookiePath() != null) {
			String path = getConfig().getCookiePath();
			if (path == null) {
				path = "/";
			}
			else if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return path;
		}
		else {
			try {
				return getServerPath();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public List<WebFragment> getWebFragments() {
		return getConfig().getWebFragments();
	}

	// always relative to whatever the web application says because it _is_ the web application
	@Override
	public String getRelativePath() {
		return "/";
	}
}
