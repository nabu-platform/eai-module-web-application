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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;

import be.nabu.eai.authentication.api.PasswordAuthenticator;
import be.nabu.eai.authentication.api.SecretAuthenticator;
import be.nabu.eai.authentication.api.TypedAuthenticator;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.module.http.server.RepositoryExceptionFormatter;
import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.module.types.structure.StructureManager;
import be.nabu.eai.module.web.application.WebConfiguration.WebConfigurationPart;
import be.nabu.eai.module.web.application.api.ArbitraryAuthenticator;
import be.nabu.eai.module.web.application.api.BearerAuthenticator;
import be.nabu.eai.module.web.application.api.CORSHandler;
import be.nabu.eai.module.web.application.api.FeaturedWebArtifact;
import be.nabu.eai.module.web.application.api.RESTFragment;
import be.nabu.eai.module.web.application.api.RESTFragmentProvider;
import be.nabu.eai.module.web.application.api.RateLimitProvider;
import be.nabu.eai.module.web.application.api.RequestLanguageProvider;
import be.nabu.eai.module.web.application.api.RequestSubscriber;
import be.nabu.eai.module.web.application.api.RobotEntry;
import be.nabu.eai.module.web.application.api.TemporaryAuthenticationGenerator;
import be.nabu.eai.module.web.application.api.TemporaryAuthenticationRevoker;
import be.nabu.eai.module.web.application.api.TemporaryAuthenticator;
import be.nabu.eai.module.web.application.cors.CORSListener;
import be.nabu.eai.module.web.application.cors.CORSPostProcessor;
import be.nabu.eai.module.web.application.resource.WebApplicationResourceResolver;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.MetricsLevelProvider;
import be.nabu.eai.repository.api.AuthenticatorProvider;
import be.nabu.eai.repository.api.CacheProviderArtifact;
import be.nabu.eai.repository.api.Documented;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.LanguageProvider;
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
import be.nabu.glue.api.AssignmentExecutor;
import be.nabu.glue.api.ExecutionContext;
import be.nabu.glue.api.Executor;
import be.nabu.glue.api.ExecutorGroup;
import be.nabu.glue.api.ParserProvider;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.api.StringSubstituter;
import be.nabu.glue.api.StringSubstituterProvider;
import be.nabu.glue.core.impl.DefaultOptionalTypeProvider;
import be.nabu.glue.core.impl.methods.v2.HashMethods;
import be.nabu.glue.core.impl.parsers.GlueParserProvider;
import be.nabu.glue.core.impl.providers.StaticJavaMethodProvider;
import be.nabu.glue.core.repositories.ScannableScriptRepository;
import be.nabu.glue.impl.ForkedExecutionContext;
import be.nabu.glue.impl.ImperativeSubstitutor;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.glue.types.GlueTypeUtils;
import be.nabu.glue.utils.MultipleRepository;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.glue.utils.ScriptUtils;
import be.nabu.libs.artifacts.FeatureImpl;
import be.nabu.libs.artifacts.api.CommitableArtifact;
import be.nabu.libs.artifacts.api.Feature;
import be.nabu.libs.artifacts.api.FeaturedArtifact;
import be.nabu.libs.artifacts.api.StartableArtifact;
import be.nabu.libs.artifacts.api.StoppableArtifact;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.DeviceValidator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.PotentialPermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.SecretGenerator;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.authentication.impl.TimeoutTokenValidator;
import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.cache.api.CacheProvider;
import be.nabu.libs.cache.impl.AccessBasedTimeoutManager;
import be.nabu.libs.cache.impl.LastModifiedTimeoutManager;
import be.nabu.libs.cache.impl.StringSerializer;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.events.filters.AndEventFilter;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.ContentRewriter;
import be.nabu.libs.http.api.HTTPEntity;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.api.server.SessionProvider;
import be.nabu.libs.http.api.server.SessionResolver;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.glue.GluePostProcessListener;
import be.nabu.libs.http.glue.GluePreprocessListener;
import be.nabu.libs.http.glue.GlueScriptCallValidator;
import be.nabu.libs.http.glue.GlueSessionResolver;
import be.nabu.libs.http.glue.GlueWebParserProvider;
import be.nabu.libs.http.glue.HTTPEntityDataSerializer;
import be.nabu.libs.http.glue.api.CacheKeyProvider;
import be.nabu.libs.http.glue.impl.GlueScriptCacheRefresher;
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
import be.nabu.libs.metrics.core.filters.ThresholdOverTimeFilter;
import be.nabu.libs.metrics.impl.MetricGrouper;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.SourceContext;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.CombinedContainer;
import be.nabu.libs.resources.ResourceFactory;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.TimestampedResource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceLevelAgreement;
import be.nabu.libs.services.api.ServiceLevelAgreementProvider;
import be.nabu.libs.services.fixed.FixedInputService;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.TypeBaseUtils;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.map.MapContent;
import be.nabu.libs.types.map.MapTypeGenerator;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.StructureGenerator;
import be.nabu.utils.cep.api.EventSeverity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.ByteBufferFactory;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiableHeader;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;
import be.nabu.utils.security.PBEAlgorithm;
import be.nabu.utils.security.SecurityUtils;

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
public class WebApplication extends JAXBArtifact<WebApplicationConfiguration> implements StartableArtifact, StoppableArtifact, AuthenticatorProvider, WebFragmentProvider, RESTFragmentProvider, FeaturedArtifact, CommitableArtifact, ServiceLevelAgreementProvider {

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
	private Authenticator authenticator;
	private RoleHandler roleHandler;
	
	private boolean passwordAuthenticatorResolved, secretAuthenticatorResolved, authenticatorResolved, roleHandlerResolved, permissionHandlerResolved, potentialPermissionHandlerResolved, tokenValidatorResolved, languageProviderResolved, userLanguageProviderResolved, deviceValidatorResolved,
		translatorResolved, bearerAuthenticatorResolved;
	private PermissionHandler permissionHandler;
	private PotentialPermissionHandler potentialPermissionHandler;
	private TokenValidator tokenValidator;
	private UserLanguageProvider userLanguageProvider;
	private DeviceValidator deviceValidator;
	private Translator translator;
	private BearerAuthenticator bearerAuthenticator;
	
	private boolean secretGeneratorResolved;
	private SecretGenerator secretGenerator;
	
	// typeId > regex > content
	private Map<String, Map<String, ComplexContent>> fragmentConfigurations;
	// very bad solution to keep track of which configuration is environment specific (almost none are)
	private List<ComplexContent> environmentSpecificConfigurations = new ArrayList<ComplexContent>();
	private String version;
	private GlueWebParserProvider parserProvider;
	private boolean rateLimiterResolved;
	private RateLimitProvider rateLimitProvider;
	private List<RESTFragment> restFragments;
	
	private List<RobotEntry> robotEntries = new ArrayList<RobotEntry>();
	private LanguageProvider languageProvider;
	private boolean requestLanguageProviderResolved;
	private RequestLanguageProvider requestLanguageProvider;
	private boolean temporaryAuthenticatorResolved;
	private TemporaryAuthenticator temporaryAuthenticator;
	private boolean temporaryAuthenticationGeneratorResolved;
	private TemporaryAuthenticationGenerator temporaryAuthenticationGenerator;
	private boolean temporaryAuthenticationRevokerResolved;
	private TemporaryAuthenticationRevoker temporaryAuthenticationRevoker;
	private boolean corsHandlerResolved;
	private CORSHandler corsHandler;
	// in prd mode the cache is not updated (normally), so once we calculate the last update, we can keep track of that date to prevent further I/O
	private Date lastCacheUpdate;
	
	// the permission necessary to be able to update the service context for a call
	// you can also be more specific, for instance if you can only set the "telemetrics" context but not the "fira" context, you might set a permission $application.setServiceContext.telemetrics
	// if the broad one is not allowed, the specific one is checked
	// the context remains the web application id
	public static final String PERMISSION_UPDATE_SERVICE_CONTEXT = "context.switch";
	
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
		if (getConfig().getVirtualHost() != null && getConfig().getVirtualHost().getServer() != null) {
			HTTPServer server = getConfiguration().getVirtualHost().getServer().getServer();
			if (server != null && server.getExceptionFormatter() instanceof RepositoryExceptionFormatter) {
				((RepositoryExceptionFormatter) server.getExceptionFormatter()).unregister(getId());
			}
		}
		// @2022-03-22: due to a bug clustered caches were never reset correctly on shutdown. this bug has been fixed but we wanted to keep this behavior, in the future we might reenable it with a checkbox 
//		if (getConfiguration().getSessionCacheProvider() != null) {
//			getConfiguration().getSessionCacheProvider().remove(getConfig().getSessionCacheId() != null ? getConfig().getSessionCacheId() : getId() + "-session");
//		}
		List<WebFragment> webFragments = getConfiguration().getWebFragments();
		if (webFragments != null) {
			for (WebFragment fragment : webFragments) {
				if (fragment != null) {
					fragment.stop(this, null);
				}
			}
		}
	}

	private static volatile boolean registeredResolver;
	private ArrayList<Feature> availableFeatures;
	private boolean arbitraryAuthenticatorResolved;
	private ArbitraryAuthenticator arbitraryAuthenticator;
	private PasswordAuthenticator passwordAuthenticator;
	private SecretAuthenticator secretAuthenticator;
	private boolean typedAuthenticatorResolved;
	private TypedAuthenticator typedAuthenticator;
	
	// we keep track in production mode of which optimized versions we have checked and which turned up nothing
	private Map<String, byte[]> checkedOptimizations = new HashMap<String, byte[]>();
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void start() throws IOException {
		// the factory only picks up new resolvers initially
		// but the nabu repository uses the resource system to actually load everything
		// so by the time all the modules are loaded, the resolver instance does not accept any newcomers
		// so we manually register it here
		// TODO: maybe use a better process?
		if (!registeredResolver) {
			synchronized(this) {
				if (!registeredResolver) {
					ResourceFactory.getInstance().addResourceResolver(new WebApplicationResourceResolver());
					registeredResolver = true;
				}
			}
		}
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

			ResourceContainer<?> localPublicDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PUBLIC);
			ResourceContainer<?> localPrivateDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PRIVATE);
			
			if (EAIResourceRepository.isDevelopment() && getDirectory() instanceof ManageableContainer) {
				if (localPublicDirectory == null) {
					localPublicDirectory = (ResourceContainer<?>) ((ManageableContainer<?>) getDirectory()).create(EAIResourceRepository.PUBLIC, Resource.CONTENT_TYPE_DIRECTORY);
				}
				if (localPrivateDirectory == null) {
					localPrivateDirectory = (ResourceContainer<?>) ((ManageableContainer<?>) getDirectory()).create(EAIResourceRepository.PRIVATE, Resource.CONTENT_TYPE_DIRECTORY);
				}
			}
			
			final ResourceContainer<?> publicDirectory = localPublicDirectory;
			final ResourceContainer<?> privateDirectory = localPrivateDirectory;
			
			ResourceContainer<?> meta = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("meta");
			ScriptRepository metaRepository = null;
			if (meta != null) {
				metaRepository = new ScannableScriptRepository(null, meta, getParserProvider(), Charset.defaultCharset());
			}
			
			repository = new MultipleRepository(null);
			List<ContentRewriter> rewriters = new ArrayList<ContentRewriter>();
			
			// add whitelisted codes to the server exception formatter
			if (getConfiguration().getVirtualHost().getServer() != null) {
				HTTPServer server = getConfiguration().getVirtualHost().getServer().getServer();
				if (server.getExceptionFormatter() instanceof RepositoryExceptionFormatter && getConfiguration().getWhitelistedCodes() != null) {
					((RepositoryExceptionFormatter) server.getExceptionFormatter()).register(getId(), Arrays.asList(getConfiguration().getWhitelistedCodes().split("[\\s]*,[\\s]*")));
				}
			}
			
			// TODO: if we have a jwt token, register a listener early on that can spot a Authorization: Bearer <token> header
			// and transform it into a security header

			// reuse the sessions from another application
			if (licensed && getConfig().getSessionProviderApplication() != null) {
				String providerPath = getConfig().getSessionProviderApplication().getConfig().getPath();
				String currentPath = getConfig().getPath();
				if (currentPath == null) {
					logger.warn("The application " + getId() + " does not have a path, this means it will live on the root. Make sure you do response rewriting to share session cookies with: " + getConfig().getSessionProviderApplication().getId());
				}
				else if (providerPath != null && !currentPath.startsWith(providerPath)) {
					logger.warn("The application " + getId() + " does not share a path with the provider application " + getConfig().getSessionProviderApplication().getId() + ". Make sure you do response rewriting to share session cookies.");
				}
				sessionProvider = getConfig().getSessionProviderApplication().getSessionProvider();
			}
			// create session provider
			else if (licensed && getConfiguration().getSessionCacheProvider() != null) {
				Cache sessionCache = getConfiguration().getSessionCacheProvider().create(
					getConfig().getSessionCacheId() == null ? getId() + "-session" : getConfig().getSessionCacheId(),
					// defaults to unlimited
					getConfiguration().getMaxTotalSessionSize() == null ? 0 : getConfiguration().getMaxTotalSessionSize(),
					// defaults to unlimited
					getConfiguration().getMaxSessionSize() == null ? 0 : getConfiguration().getMaxSessionSize(),
					new StringSerializer(),
					// not everything is serializable, especially complex contents with links to complex types
					//new SerializableSerializer(),

					// we can't use XML serialization as there is no defined type, so serializing works but we can't deserialize
					// the XML deserialization process does have support for dynamic type extensions through xsi:type, it does not however have support for entirely dynamic content like JSON
					// perhaps if this was added, the XML deserialization would also work (still need cast to map though)
					// new ObjectSerializer(),
					
					// we can't use JSON because the result returned from the cache is not a map but instead a generated structure (mapcontent, structure instance,...) generated by the json unmarshaller
					//new ObjectSerializer(new ComplexContentJSONSerializer()),
					
					// we can't use the map serializer because the root returned content is a map (yay) but all the children are not correctly unmarshalled because we have no definition
					// that means all children are generic map objects rather than for example a token
					//new MapSerializer(),
					
					new UndefinedSerializer(new MapTypeGenerator(), new Function<Object, Object>() {
						@Override
						public Object apply(Object t) {
							// we know the sessions use maps, as CacheSession is a known implementation
							return ((MapContent) t).toMap();
						}
					}),
					// no refresher obviously
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
			
			Map<String, String> environment = getEnvironmentProperties();
			
			String environmentName = serverPath;
			if (environmentName.startsWith("/")) {
				environmentName.substring(1);
			}
			if (environmentName.isEmpty()) {
				environmentName = "root";
			}
			
			EventDispatcher dispatcher = getConfiguration().getVirtualHost().getDispatcher();
			
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
			
			// allow custom request handlers, request rewriting & response rewriting are the domain of the glue pre & post processors
			if (getConfiguration().getRequestSubscriber() != null) {
				final RequestSubscriber requestSubscriber = POJOUtils.newProxy(RequestSubscriber.class, wrap(getConfiguration().getRequestSubscriber(), getMethod(RequestSubscriber.class, "handle")), getRepository(), SystemPrincipal.ROOT);
				EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
					@Override
					public HTTPResponse handle(HTTPRequest event) {
						// if there is no pipeline context, the request is not coming in over http
						if (PipelineUtils.getPipeline() == null) {
							return null;
						}
						SourceContext sourceContext = PipelineUtils.getPipeline().getSourceContext();
						return requestSubscriber.handle(getId(), getRealm(), new SourceImpl(sourceContext), event);
					}
				});
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
			}
			
			// before the base authentication required authenticate header rewriter, add a rewriter for the response (if applicable)
			if (metaRepository != null) {
				subscriptions.add(registerPostProcessor(metaRepository, environment, environmentName));
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
						Header etag = MimeUtils.getHeader("ETag", event.getContent().getHeaders());
						if (lastModified == null && etag == null) {
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
			
			CORSHandler corsHandler = getCORSHandler();
			if (corsHandler != null) {
				EventSubscription<HTTPRequest, HTTPResponse> corsSubscription = dispatcher.subscribe(HTTPRequest.class, new CORSListener(this, corsHandler));
				corsSubscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(corsSubscription);
				
				EventSubscription<HTTPResponse, HTTPResponse> corsPostProcess = dispatcher.subscribe(HTTPResponse.class, new CORSPostProcessor());
				corsPostProcess.filter(HTTPServerUtils.limitToRequestPath(serverPath));
				subscriptions.add(corsPostProcess);
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
			
			BearerAuthenticator bearerAuthenticator = getBearerAuthenticator();
			if (bearerAuthenticator != null) {
				BearerAuthenticatorHandler bearerAuthenticatorHandler = new BearerAuthenticatorHandler(this, bearerAuthenticator, getRealm());
				EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
					@Override
					public HTTPResponse handle(HTTPRequest event) {
						try {
							// set it globally
							ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
							ServiceRuntime.getGlobalContext().put("service.context", getId());
							ServiceRuntime.getGlobalContext().put("webApplicationId", getId());
							ServiceRuntime.getGlobalContext().put("service.source", "web");
							return bearerAuthenticatorHandler.handle(event);
						}
						catch (HTTPException e) {
							e.getContext().add(getId());
							throw e;
						}
						finally {
							// unset it
							ServiceRuntime.setGlobalContext(null);	
						}
					}
				});
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(subscription);
			}
			
			ArbitraryAuthenticator arbitraryAuthenticator = getArbitraryAuthenticator();
			if (arbitraryAuthenticator != null) {
				ArbitraryAuthenticatorHandler arbitraryAuthenticatorHandler = new ArbitraryAuthenticatorHandler(this, arbitraryAuthenticator, getRealm());
				EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
					@Override
					public HTTPResponse handle(HTTPRequest event) {
						try {
							// set it globally
							ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
							ServiceRuntime.getGlobalContext().put("service.context", getId());
							ServiceRuntime.getGlobalContext().put("webApplicationId", getId());
							ServiceRuntime.getGlobalContext().put("service.source", "web");
							return arbitraryAuthenticatorHandler.handle(event);
						}
						catch (HTTPException e) {
							e.getContext().add(getId());
							throw e;
						}
						finally {
							// unset it
							ServiceRuntime.setGlobalContext(null);	
						}
					}
				});
				subscription.filter(HTTPServerUtils.limitToPath(serverPath));
				subscriptions.add(subscription);
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
				subscriptions.add(registerPreProcessor(metaRepository, environment, environmentName));
			}
			
			// we start the web fragments that have highest priority
			for (WebFragment fragment : webFragments) {
				if (fragment != null) {
					if (fragment.getPriority() == WebFragmentPriority.HIGHEST) {
						fragment.start(this, null);
					}
					else {
						break;
					}
				}
			}
			
			ParserProvider parserProvider = getParserProvider();
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
//				ResourceContainer<?> providedPublicResources = (ResourceContainer<?>) publicDirectory.getChild("provided/resources");
//				if (providedPublicResources != null) {
//					resources = resources == null ? (ResourceContainer<?>) providedPublicResources : new CombinedContainer(null, "resources", resources, (ResourceContainer<?>) providedPublicResources);
//				}
				ResourceContainer<?> pages = (ResourceContainer<?>) publicDirectory.getChild("pages");
				if (pages != null) {
					logger.debug("Adding public scripts found in: " + pages);
					// the configured charset is for the end user, NOT for the local glue scripts, that should be the system default
					ScannableScriptRepository scannableScriptRepository = new ScannableScriptRepository(repository, pages, parserProvider, Charset.defaultCharset());
					scannableScriptRepository.setGroup(GlueListener.PUBLIC);
					repository.add(scannableScriptRepository);
				}
//				ResourceContainer<?> providedArtifacts = (ResourceContainer<?>) publicDirectory.getChild("provided/artifacts");
//				if (providedArtifacts != null) {
//					repository.add(new ScannableScriptRepository(repository, (ResourceContainer<?>) providedArtifacts, parserProvider, Charset.defaultCharset()));
//				}
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
			listener.setOfflineChecker(new Predicate<HTTPRequest>() {
				@Override
				public boolean test(HTTPRequest t) {
					WebApplicationUtils.checkOffline(WebApplication.this, t);
					return false;
				}
			});
			// we handle this at a higher level
			listener.setAllowEncoding(false);
			
			listener.setSecureCookiesOnly(isSecure());
			listener.setDefaultCookieSitePolicy(getConfig().getDefaultCookieSitePolicy());
			
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
									Script script = null;
									try {
										script = listener.getRepository().getScript(name);
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
										new HTTPEntityDataSerializer(),
										new GlueScriptCacheRefresher(listener, name), 
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
			
			final UserLanguageProvider languageProvider = getUserLanguageProvider();
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
						Token token = UserMethods.token();
						String languageKey = "userLanguage";
						if (token == null) {
							languageKey += "-anonymous";
						}
						else {
							languageKey += "-" + token.getRealm() + "-" + token.getName();
						}
						String language = (String) runtime.getContext().get(languageKey);
						// if we have a language provider, try to get it from there
						if (language == null && languageProvider != null) {
							// the token may be null then the provider can send back a default language
							language = languageProvider.getLanguage(token);
							ExecutionContext executionContext = ScriptRuntime.getRuntime().getExecutionContext();
							while (executionContext instanceof ForkedExecutionContext) {
								executionContext = ((ForkedExecutionContext) executionContext).getParent();
							}
						}
						// if there is no language yet, check if a language cookie has been set
						if (language == null && RequestMethods.entity() != null && RequestMethods.entity().getContent() != null && !getConfig().isIgnoreLanguageCookie()) {
							Map<String, List<String>> cookies = HTTPUtils.getCookies(RequestMethods.entity().getContent().getHeaders());
							if (cookies != null && cookies.get("language") != null && !cookies.get("language").isEmpty()) {
								language = cookies.get("language").get(0);
							}
						}
						RequestLanguageProvider requestLanguageProvider = null;
						try {
							requestLanguageProvider = getRequestLanguageProvider();
						}
						catch (IOException e) {
							logger.error("Could not get request language provider", e);
						}
						if (language == null && getConfig().isForceRequestLanguage() && requestLanguageProvider != null) {
							HTTPEntity entity = ScriptRuntime.getRuntime() == null ? null : (HTTPEntity) ScriptRuntime.getRuntime().getContext().get(RequestMethods.ENTITY);
							if (entity instanceof HTTPRequest) {
								language = requestLanguageProvider.getLanguage((HTTPRequest) entity);
							}
						}
						// if there is no language yet, try to detect from browser settings
						if (language == null) {
							if (RequestMethods.entity() != null && RequestMethods.entity().getContent() != null) {
								List<String> acceptedLanguages = MimeUtils.getAcceptedLanguages(RequestMethods.entity().getContent().getHeaders());
								if (!acceptedLanguages.isEmpty()) {
									LanguageProvider provider = getLanguageProvider();
									List<String> supportedLanguages = provider == null ? null : provider.getSupportedLanguages();
									for (String acceptedLanguage : acceptedLanguages) {
										String potential = acceptedLanguage.replaceAll("-.*$", "");
										if (supportedLanguages == null || supportedLanguages.contains(potential)) {
											language = potential;
											break;
										}
									}
								}
							}
						}
						if (language == null && !getConfig().isForceRequestLanguage() && requestLanguageProvider != null) {
							HTTPEntity entity = ScriptRuntime.getRuntime() == null ? null : (HTTPEntity) ScriptRuntime.getRuntime().getContext().get(RequestMethods.ENTITY);
							if (entity instanceof HTTPRequest) {
								language = requestLanguageProvider.getLanguage((HTTPRequest) entity);
							}
						}
						
						if (language != null) {
							runtime.getContext().put(languageKey, language);
						}
						try {
							if (additional != null) {
								// in the olden days instead of null as default category, we passed in: \"page:" + ScriptUtils.getFullName(runtime.getScript()) + "\")
								// however, because of concatting and possible other processing, the runtime script name rarely has a relation to the context anymore
								// it is clearer to work without a context then allowing for cross-context translations
								// the first part is always captured in group 1 and the second always in group 2, but only one actually contains a value so printing them both should not be a problem
								return new be.nabu.glue.impl.ImperativeSubstitutor("%", "script.template(" + getConfiguration().getTranslationService().getId() 
										+ "(control.when(\"${value}\" ~ \"^(?:.*?::|[a-zA-Z0-9.]+:).*\", string.replace(\"^(?:(.*?)::|([a-zA-Z0-9.]+):).*\", \"$1$2\", \"${value}\"), null), "
										+ "control.when(\"${value}\" ~ \"^(?:.*?::|[a-zA-Z0-9.]+:).*\", string.replace(\"^(?:.*?::|[a-zA-Z0-9.]+:)(.*)\", \"$1\", \"${value}\"), \"${value}\"), " + (language == null ? "null" : "\"" + language + "\"")
										+ ", " + key + ": json.objectify('" + additional + "'))/translation)");
							}
							else {
								return new be.nabu.glue.impl.ImperativeSubstitutor("%", "script.template(" + getConfiguration().getTranslationService().getId() 
										+ "(control.when(\"${value}\" ~ \"^(?:.*?::|[a-zA-Z0-9.]+:).*\", string.replace(\"^(?:(.*?)::|([a-zA-Z0-9.]+):).*\", \"$1$2\", \"${value}\"), null), "
										+ "control.when(\"${value}\" ~ \"^(?:.*?::|[a-zA-Z0-9.]+:).*\", string.replace(\"^(?:.*?::|[a-zA-Z0-9.]+:)(.*)\", \"$1\", \"${value}\"), \"${value}\"), " + (language == null ? "null" : "\"" + language + "\"") + ")/translation)");
							}
						}
						catch (IOException e) {
							throw new RuntimeException("Could not get translation service", e);
						}
					}
				});
			}
			// by default, replace the to-be-translated bits with the original value
			else {
				listener.getSubstituterProviders().add(new StringSubstituterProvider() {
					@Override
					public StringSubstituter getSubstituter(ScriptRuntime runtime) {
						// we used to have a restrictive [a-zA-Z0-9.]+: to indicate contexts
						// this is too fragile however, so we are moving to any text followed by two ::
						return new be.nabu.glue.impl.ImperativeSubstitutor("%", "template("
							+ "when(\"${value}\" ~ \"^(?:.*?::|[a-zA-Z0-9.]+:).*\", replace(\"^(?:.*?::|[a-zA-Z0-9.]+:)(.*)\", \"$1\", \"${value}\"), \"${value}\"))");
					}
				});
			}
			// set an additional cache key provider that can choose the user language
			if (getConfiguration().getTranslationService() != null) {
				listener.addCacheKeyProvider(new CacheKeyProvider() {
					@Override
					public String getAdditionalCacheKey(HTTPRequest request, Token token, Script script) {
						// make sure we mirror the logic above for the substitution cause that will be the actual language in the returned content
						String language = languageProvider == null ? null : languageProvider.getLanguage(token);
						// if there is no language yet, check if a language cookie has been set
						if (language == null && request != null && request.getContent() != null && !getConfig().isIgnoreLanguageCookie()) {
							Map<String, List<String>> cookies = HTTPUtils.getCookies(request.getContent().getHeaders());
							if (cookies != null && cookies.get("language") != null && !cookies.get("language").isEmpty()) {
								language = cookies.get("language").get(0);
							}
						}
						RequestLanguageProvider requestLanguageProvider = null;
						try {
							requestLanguageProvider = getRequestLanguageProvider();
						}
						catch (IOException e) {
							logger.error("Could not get request language provider", e);
						}
						if (language == null && getConfig().isForceRequestLanguage() && requestLanguageProvider != null) {
							language = requestLanguageProvider.getLanguage(request);
						}
						if (language == null) {
							List<String> acceptedLanguages = MimeUtils.getAcceptedLanguages(request.getContent().getHeaders());
							if (!acceptedLanguages.isEmpty()) {
								LanguageProvider provider = getLanguageProvider();
								List<String> supportedLanguages = provider == null ? null : provider.getSupportedLanguages();
								for (String acceptedLanguage : acceptedLanguages) {
									String potential = acceptedLanguage.replaceAll("-.*$", "");
									if (supportedLanguages == null || supportedLanguages.contains(potential)) {
										language = potential;
										break;
									}
								}
							}
						}
						if (language == null && !getConfig().isForceRequestLanguage() && requestLanguageProvider != null) {
							HTTPEntity entity = ScriptRuntime.getRuntime() == null ? null : (HTTPEntity) ScriptRuntime.getRuntime().getContext().get(RequestMethods.ENTITY);
							if (entity instanceof HTTPRequest) {
								language = requestLanguageProvider.getLanguage((HTTPRequest) entity);
							}
						}
						return language == null ? null : "lang=" + language;
					}
				});
			}
			
			// you can set the "@throttle" annotation to explicitly request a throttle, we don't do it by default
			if (getRateLimiter() != null) {
				listener.setScriptCallValidator(new GlueScriptCallValidator() {
					@Override
					public HTTPResponse validate(HTTPRequest request, Token token, Device device, Script script) {
						try {
							boolean throttle = script.getRoot() != null && script.getRoot().getContext() != null && script.getRoot().getContext().getAnnotations() != null
									&& script.getRoot().getContext().getAnnotations().containsKey("throttle")
									&& !"false".equals(script.getRoot().getContext().getAnnotations().get("throttle"));
							if (throttle) {
								String operationId = script.getRoot() != null && script.getRoot().getContext() != null && script.getRoot().getContext().getAnnotations() != null
									? script.getRoot().getContext().getAnnotations().get("operationId")
									: null;
								if (operationId == null) {
									operationId = ScriptUtils.getFullName(script);
								}
								return WebApplicationUtils.checkRateLimits(WebApplication.this, token, device, operationId, null, request);
							}
							else {
								return null;
							}
						}
						catch (Exception e) {
							logger.error("Could not check rate limiting for script", e);
							HTTPException httpException = new HTTPException(500, e, token);
							httpException.getContext().add(getId());
							throw httpException;
						}
					}
				});
			}
			
			listener.getContentRewriters().addAll(rewriters);
			listener.setRefreshScripts(isDevelopment);
			// managed at higher level now...
//			listener.setAllowEncoding(!isDevelopment && getConfig().isAllowContentEncoding());
			listener.setAuthenticator(authenticator);
			listener.setTokenValidator(getTokenValidator());
			listener.setPermissionHandler(getPermissionHandler());
			listener.setRoleHandler(getRoleHandler());
			listener.setDeviceValidator(getDeviceValidator());
			listener.setRealm(realm);
			listener.setSecretGenerator(getSecretGenerator());
			// always creating a session can create other issues
//			listener.setAlwaysCreateSession(true);
			
			EventSubscription<HTTPRequest, HTTPResponse> robotSubscription = dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
				@Override
				public HTTPResponse handle(HTTPRequest request) {
					if (robotEntries.isEmpty()) {
						return null;
					}
					try {
						URI uri = HTTPUtils.getURI(request, false);
						if (uri.getPath().equals("/robots.txt")) {
							// should be ascii compliant
							byte [] content = getRobotsContent().getBytes(Charset.forName("UTF-8"));
							return new DefaultHTTPResponse(request, 200, HTTPCodes.getMessage(200), new PlainMimeContentPart(null, IOUtils.wrap(content, true),
								new MimeHeader("Content-Length", "" + content.length),
								new MimeHeader("Content-Type", "text/plain")
							));
						}
					}
					catch (Exception e) {
						HTTPException httpException = new HTTPException(500, e);
						httpException.getContext().add(getId());
						throw httpException;	
					}
					return null;
				}
			});
			subscriptions.add(robotSubscription);
			
//			EventSubscription<HTTPRequest, HTTPResponse> heartbeatSubscription = dispatcher.subscribe(HTTPRequest.class, new HeartbeatListener(this.getConfig().getVirtualHost()));
//			subscriptions.add(heartbeatSubscription);
			
			// first start everything above normal priority
			for (WebFragment fragment : webFragments) {
				if (fragment != null) {
					if (fragment.getPriority().compareTo(WebFragmentPriority.NORMAL) < 0 && fragment.getPriority() != WebFragmentPriority.HIGHEST) {
						fragment.start(this, null);
					}
					else if (fragment.getPriority().compareTo(WebFragmentPriority.NORMAL) >= 0) {
						break;
					}
				}
			}
			
			// TODO: IF you have a script cache configured, we should save the loaded data there, this can allow for resets (?)
			// this needs to be part of a bigger push to recalculate everything though, otherwise we just keep picking up from the hard cache
			// if we are in production mode and we have optimize load turned on, check if we have a cache folder
			ResourceContainer<?> cacheFolder = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("cache");
			if (getConfig().isOptimizedLoad()) {
				// if we do have a cache folder, we turned on optimize in development
				if (cacheFolder != null || EAIResourceRepository.isDevelopment()) {
					EventSubscription<HTTPRequest, HTTPResponse> optimizationSubscription = dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
						@Override
						public HTTPResponse handle(HTTPRequest event) {
							// the cachefolder should only be null in dev at this point, we want to give it a chance to be created though
							if (cacheFolder == null) {
								ResourceContainer<?> localCacheFolder = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("cache");
								if (localCacheFolder != null) {
									return getOptimizedResponse(localCacheFolder, event, null);		
								}
								else {
									return null;
								}
							}
							return getOptimizedResponse(cacheFolder, event, null);
						}
					});
					optimizationSubscription.filter(HTTPServerUtils.limitToPath(serverPath));
					subscriptions.add(optimizationSubscription);
				}
			}
			
			// start the glue listener
			//EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, listener);
			EventSubscription<HTTPRequest, HTTPResponse> subscription = dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
				@Override
				public HTTPResponse handle(HTTPRequest event) {
					try {
						// set it globally
						ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
						ServiceRuntime.getGlobalContext().put("service.context", getId());
						ServiceRuntime.getGlobalContext().put("webApplicationId", getId());
						ServiceRuntime.getGlobalContext().put("service.source", "glue");
						HTTPResponse response = listener.handle(event);
						response = optimizeResponse(privateDirectory, event, response, null);
						return response;
					}
					catch (HTTPException e) {
						e.getContext().add(getId());
						throw e;
					}
					finally {
						// unset it
						ServiceRuntime.setGlobalContext(null);	
					}
				}

				
			});
			subscription.filter(HTTPServerUtils.limitToPath(serverPath));
			subscriptions.add(subscription);
			// start remaining fragments
			for (WebFragment fragment : webFragments) {
				if (fragment != null && fragment.getPriority().compareTo(WebFragmentPriority.NORMAL) >= 0) {
					fragment.start(this, null);
				}
			}
			
			// add all the services at the end
			if (getConfig().getServices() != null) {
				for (DefinedService service : getConfig().getServices()) {
					if (service == null) {
						continue;
					}
					RESTServiceListener listener = new RESTServiceListener(this, RESTServiceListener.asRestFragment(this, service), service);
					EventSubscription<HTTPRequest, HTTPResponse> serviceSubscription = getDispatcher().subscribe(HTTPRequest.class, listener);
					serviceSubscription.filter(HTTPServerUtils.limitToPath(serverPath));
					subscriptions.add(serviceSubscription);
				}
			}
			
			// if we have enabled html 5 mode, return the index for all calls that allow for text/html and have no response
			if (getConfig().isHtml5Mode()) {
				EventSubscription<HTTPRequest, HTTPResponse> html5Subscription = dispatcher.subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
					@Override
					public HTTPResponse handle(HTTPRequest event) {
						// if we get here without a response, send back the index page
						if (event.getContent() != null && event.getMethod().equalsIgnoreCase("get")) {
							List<String> acceptedContentTypes = MimeUtils.getAcceptedContentTypes(event.getContent().getHeaders());
							if (acceptedContentTypes.contains("text/html") || acceptedContentTypes.contains("text/*") || acceptedContentTypes.contains("*/*")) {
								// set it globally
								try {
									URI uri = HTTPUtils.getURI(event, false);
									// if we find an extension, only html is supported
									// otherwise we might intercept resources that are being requested and not found (e.g. favicon.ico)
									if (!uri.getPath().endsWith("/")) {
										String fileName = uri.getPath().replaceAll("^.*/([^/]+$)", "$1");
										if (fileName.contains(".") && !fileName.endsWith(".html")) {
											return null;
										}
									}
									// check if we are at all interested
									if (uri.getPath().startsWith(getServerPath())) {
										// check that there aren't any other web applications with better matching paths
										for (WebApplication application : EAIResourceRepository.getInstance().getArtifacts(WebApplication.class)) {
											if (!application.equals(this) && application.getConfig().getVirtualHost() != null
													&& application.getConfig().getVirtualHost().equals(getConfig().getVirtualHost())
													&& (uri.getPath().startsWith(application.getServerPath() + "/") || uri.getPath().equals(application.getServerPath())) 
													&& application.getServerPath().length() > getServerPath().length()) {
												return null;
											}
										}
										event.getContent().setHeader(new MimeHeader("Original-Uri", uri.toString()));
										uri = new URI(null, null, null, -1, getServerPath(), uri.getQuery(), uri.getFragment());
										DefaultHTTPRequest rewritten = new DefaultHTTPRequest(
											"GET", 
											uri.toASCIIString(), 
											event.getContent(),
											event.getVersion()
										);
										ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
										ServiceRuntime.getGlobalContext().put("service.context", getId());
										ServiceRuntime.getGlobalContext().put("service.source", "glue");
										
										// attempt to get an optimized response
										HTTPResponse response = cacheFolder == null ? null : getOptimizedResponse(cacheFolder, rewritten, "$root");
										if (response == null) {
											response = listener.handle(rewritten);
											// we force it on the server path because of html5
											response = optimizeResponse(privateDirectory, event, response, "$root");
										}
										return response;
									}
									return null;
								}
								catch (Exception e) {
									HTTPException httpException = new HTTPException(500, e);
									httpException.getContext().add(getId());
									throw httpException;
								}
								finally {
									ServiceRuntime.setGlobalContext(null);
								}
							}
						}
						return null;
					}
				});
				subscriptions.add(html5Subscription);
			}
			// if you have custom robot handling, it should win
			robotSubscription.demote();
			started = true;
			logger.info("Started " + subscriptions.size() + " subscriptions");
		}
	}
	
	public void clearOptimizedCache() throws IOException {
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			ResourceContainer<?> cacheFolder = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("cache");
			if (cacheFolder != null) {
				List<Resource> resourcesToDelete = new ArrayList<Resource>();
				for (Resource resource : cacheFolder) {
					resourcesToDelete.add(resource);
				}
				for (Resource resource : resourcesToDelete) {
					((ManageableContainer<?>) cacheFolder).delete(resource.getName());
				}
			}
		}
		checkedOptimizations.clear();
	}
	
	private HTTPResponse getOptimizedResponse(ResourceContainer<?> cacheFolder, HTTPRequest event, String forcedName) {
		DefaultHTTPResponse response = null;
		try {
			// only gets can be optimized!!
			boolean optimized = getConfig().isOptimizedLoad() && cacheFolder != null && event.getMethod().equalsIgnoreCase("get");
			
			// if we want to optimize and we are in development, we need to check caching headers
			if (optimized && EAIResourceRepository.isDevelopment()) {
				// in most cases the static files do not change much (even in dev)
				// so by default we will send (even in dev) the cached version
				// an explicit cache reset can be triggered as needed
				boolean optimizeDev = Boolean.parseBoolean(System.getProperty("optimize.dev", "true"));
				if (!optimizeDev) {
					// if we have a cache header, we just want the latest version, we don't (currently) check the date at all
					Header cacheHeader = MimeUtils.getHeader("If-Modified-Since", event.getContent().getHeaders());
					if (cacheHeader == null) {
						optimized = false;
					}
				}
			}
			if (optimized) {
				URI uri = HTTPUtils.getURI(event, isSecure());
				String path = uri.getPath();
				if (path != null && path.startsWith(getServerPath())) {
					path = path.substring(getServerPath().length());
					if (forcedName != null) {
						path = forcedName;
					}
					else if (path == null || path.trim().isEmpty()) {
						path = "$root";
					}
					else {
						// encode so we don't have issues
						path = URIUtils.encodeURIComponent(path);
					}
		
					byte[] bytes = checkedOptimizations.get(path);
					// if we have no content but did check before, just skip, we don't want to waste i/o cycles on this
					if (bytes == null && checkedOptimizations.containsKey(path)) {
						return null;
					}
					
					Resource finalChild = null;
					String contentType = null;
					
					// the extension determines the contentType
					Resource htmlChild = cacheFolder.getChild(path + ".html");
					if (htmlChild != null) {
						contentType = "text/html";
						Resource compiledChild = cacheFolder.getChild(path + ".compiled.html");
						if (compiledChild instanceof TimestampedResource && htmlChild instanceof TimestampedResource) {
							if (((TimestampedResource) compiledChild).getLastModified().before(((TimestampedResource) htmlChild).getLastModified())) {
								// we want to check how big the difference is, if there is a limited time difference, it may be down to writing the actual files
								Date lastModifiedRaw = ((TimestampedResource) htmlChild).getLastModified();
								Date lastModifiedCompiled = ((TimestampedResource) compiledChild).getLastModified();
								long timeDiff = Math.abs(lastModifiedRaw.getTime() - lastModifiedCompiled.getTime());
								if (timeDiff > 1000l * 60) {
									compiledChild = null;
								}
							}
						}
						finalChild = compiledChild != null ? compiledChild : htmlChild;
					}
					else {
						Resource javascriptChild = cacheFolder.getChild(path + ".js");
						if (javascriptChild != null) {
							contentType = "application/javascript";
							// we check if there is a compiled version
							Resource compiledChild = cacheFolder.getChild(path + ".compiled.js");
							if (compiledChild instanceof TimestampedResource && javascriptChild instanceof TimestampedResource) {
								if (((TimestampedResource) compiledChild).getLastModified().before(((TimestampedResource) javascriptChild).getLastModified())) {
									// we want to check how big the difference is, if there is a limited time difference, it may be down to writing the actual files
									Date lastModifiedRaw = ((TimestampedResource) javascriptChild).getLastModified();
									Date lastModifiedCompiled = ((TimestampedResource) compiledChild).getLastModified();
									long timeDiff = Math.abs(lastModifiedRaw.getTime() - lastModifiedCompiled.getTime());
									if (timeDiff > 1000l * 60) {
										compiledChild = null;
									}
								}
							}
							finalChild = compiledChild != null ? compiledChild : javascriptChild; 
						}
						else {
							Resource cssChild = cacheFolder.getChild(path + ".css");
							if (cssChild != null) {
								contentType = "text/css";
								// we check if there is a compiled version
								Resource compiledChild = cacheFolder.getChild(path + ".compiled.css");
								if (compiledChild instanceof TimestampedResource && cssChild instanceof TimestampedResource) {
									if (((TimestampedResource) compiledChild).getLastModified().before(((TimestampedResource) cssChild).getLastModified())) {
										// we want to check how big the difference is, if there is a limited time difference, it may be down to writing the actual files
										Date lastModifiedRaw = ((TimestampedResource) cssChild).getLastModified();
										Date lastModifiedCompiled = ((TimestampedResource) compiledChild).getLastModified();
										long timeDiff = Math.abs(lastModifiedRaw.getTime() - lastModifiedCompiled.getTime());
										if (timeDiff > 1000l * 60) {
											compiledChild = null;
										}
									}
								}
								finalChild = compiledChild != null ? compiledChild : cssChild; 
							}
						}
					}
					// if we have a final child to stream and its content type, lets go!
					if (finalChild != null && contentType != null) {
						Date resourceLastModified = null;
						// in this case we don't set the checkedoptimizations because we don't have the bytes
						if (finalChild instanceof TimestampedResource) {
							Header lastModifiedHeader = MimeUtils.getHeader("If-Modified-Since", event.getContent().getHeaders());
							Date lastModified = lastModifiedHeader == null ? null : HTTPUtils.parseDate(lastModifiedHeader.getValue());
							resourceLastModified = ((TimestampedResource) finalChild).getLastModified();
							if (resourceLastModified != null && lastModified != null) {
								// want to get rid of ms precision...
								resourceLastModified = new Date(resourceLastModified.getTime() - (resourceLastModified.getTime() % 1000));
								// if the resource wasn't modified since your last call, let's pretend everything is ok!
								if (!resourceLastModified.after(lastModified)) {
									response = new DefaultHTTPResponse(event, 304, HTTPCodes.getMessage(304), new PlainMimeEmptyPart(null,
										new MimeHeader("Content-Length", "0"),
										// the charset is currently hardcoded
										new MimeHeader("Content-Type", contentType + "; charset=" + Charset.forName("UTF-8").name()),
										// we are not sure, let's just play it like this for now
										new MimeHeader("Cache-Control", "no-cache, must-revalidate")
									)); 
								}
							}
						}
						// if we can't respond with a 304, actually stream the bytes
						if (response == null && finalChild instanceof ReadableResource) {
							if (bytes == null) {
								ReadableContainer<ByteBuffer> readable = ((ReadableResource) finalChild).getReadable();
								try {
									bytes = IOUtils.toBytes(readable);
								}
								finally {
									readable.close();
								}
							}
							PlainMimeContentPart part = new PlainMimeContentPart(null, IOUtils.wrap(bytes, true),
								new MimeHeader("Content-Length", Integer.toString(bytes.length)),
								// the charset is currently hardcoded
								new MimeHeader("Content-Type", contentType + "; charset=" + Charset.forName("UTF-8").name()),
								// we are not sure, let's just play it like this for now
								new MimeHeader("Cache-Control", "no-cache, must-revalidate")
							);
							// we fed it bytes, it can be reopened
							part.setReopenable(true);
							response = new DefaultHTTPResponse(event, 200, HTTPCodes.getMessage(200), part);
							// save the bytes for the future!
							synchronized (checkedOptimizations) {
								checkedOptimizations.put(path, bytes);
							}
						}
						if (resourceLastModified != null && response != null) {
							response.getContent().setHeader(new MimeHeader("Last-Modified", HTTPUtils.formatDate(resourceLastModified)));
						}
						// we want to ensure that we have a device-id
						// this is mostly relevant in the initial call (the html get)
						if (response != null) {
							String deviceId = null;
							boolean isNewDevice = false;
							// check validity of device
							Device device = event.getContent() == null ? null : GlueListener.getDevice(getRealm(), event.getContent().getHeaders());
							if (device == null && deviceValidator != null) {
								device = GlueListener.newDevice(getRealm(), event.getContent().getHeaders());
								deviceId = device.getDeviceId();
								isNewDevice = true;
							}
							// if it's a new device, set it
							if (isNewDevice) {
								ModifiableHeader cookieHeader = HTTPUtils.newSetCookieHeader(
									"Device-" + getRealm(), 
									deviceId,
									new Date(new Date().getTime() + 1000l*60*60*24*365*100),
									getCookiePath(),
									// domain
									null, 
									// secure
									isSecure(),
									// http only
									true
								);
								response.getContent().setHeader(cookieHeader);
							}
						}
						if ("text/html".equals(contentType) && response != null) {
							response.getContent().setHeader(new MimeHeader("X-Frame-Options", "DENY"));
						}
					}
					else {
						// let's mark this for the future
						synchronized (checkedOptimizations) {
							checkedOptimizations.put(path, null);
						}
					}
				}
			}
		}
		catch (Exception e) {
			throw new HTTPException(500, e);
		}
		return response;
	}
	
	private HTTPResponse optimizeResponse(ResourceContainer<?> privateDirectory, HTTPRequest event, HTTPResponse response, String forcedName) {
		// in prd mode, the private directory can be null
		if (privateDirectory != null && response != null && getConfig().isOptimizedLoad() && EAIResourceRepository.isDevelopment() && response.getContent() instanceof ContentPart && event.getMethod().equalsIgnoreCase("get")) {
			String contentType = MimeUtils.getContentType(response.getContent().getHeaders());
			if (contentType != null) {
				// we want to keep a compiled version of these for easy serving in target environments
				if (contentType.trim().equals("application/javascript") || contentType.trim().equals("text/css") || contentType.trim().equals("text/html")) {
					try {
						ResourceContainer<?> cacheFolder = (ResourceContainer<?>) privateDirectory.getChild("cache");
						if (cacheFolder == null) {
							cacheFolder = (ResourceContainer<?>) ((ManageableContainer<?>) privateDirectory).create("cache", Resource.CONTENT_TYPE_DIRECTORY);
						}
						URI uri = HTTPUtils.getURI(event, isSecure());
						String path = uri.getPath();
						if (path != null && path.startsWith(getServerPath())) {
							Date date = new Date();
							path = path.substring(getServerPath().length());
							if (forcedName != null) {
								path = forcedName;
							}
							else if (path == null || path.trim().isEmpty()) {
								path = "$root";
							}
							else {
								// encode so we don't have issues
								path = URIUtils.encodeURIComponent(path);
							}
							// reset the checked stuff, we want to reload
							checkedOptimizations.remove(path);
							
							// give it a correct extension, this can be useful for compiling
							if (contentType.trim().equals("application/javascript")) {
								path += ".js";
							}
							else if (contentType.trim().equals("text/css")) {
								path += ".css";
							}
							else if (contentType.trim().equals("text/html")) {
								path += ".html";
							}
							Resource cacheFile = cacheFolder.getChild(path);
							if (cacheFile == null) {
								cacheFile = ((ManageableContainer<?>) cacheFolder).create(path, contentType);
							}
							if (cacheFile instanceof WritableResource) {
								ReadableContainer<ByteBuffer> readable = ((ContentPart) response.getContent()).getReadable();
								if (readable != null) {
									try {
										WritableContainer<ByteBuffer> writable = ((WritableResource) cacheFile).getWritable();
										try {
											// if it can be reopened, we just push it to the file system
											if (((ContentPart) response.getContent()).isReopenable()) {
												IOUtils.copyBytes(readable, writable);
											}
											// otherwise, we push it to the file system and keep a copy in memory
											else {
												ByteBuffer buffer = ByteBufferFactory.getInstance().newInstance();
												IOUtils.copyBytes(readable, IOUtils.multicast(writable, buffer));
												buffer.close();
												// set a new response that can be streamed...
												response = new DefaultHTTPResponse(response.getCode(), response.getMessage(), new PlainMimeContentPart(null, buffer, response.getContent().getHeaders()));
												
												// we want (even in dev) to use caching
												response.getContent().setHeader(new MimeHeader("Last-Modified", HTTPUtils.formatDate(date)));
											}
										}
										finally {
											writable.close();
										}
									}
									finally {
										readable.close();
									}
								}
								else {
									logger.warn("Could not retrieve readable for: " + path);
								}
							}
						}
					}
					catch (Exception e) {
						logger.warn("Could not compile cache", e);
					}
				}
			}
		}
		return response;
	}
	
	private String getFragmentPath() {
		return getConfig().getPath() == null ? "/" : getConfig().getPath();
	}

	public EventSubscription<HTTPResponse, HTTPResponse> registerPostProcessor(ScriptRepository metaRepository) {
		try {
			return this.registerPostProcessor(metaRepository, getEnvironmentProperties(), "root");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private EventSubscription<HTTPResponse, HTTPResponse> registerPostProcessor(ScriptRepository metaRepository, Map<String, String> environment, String environmentName) throws IOException {
		String serverPath = getServerPath();
		GluePostProcessListener postprocessListener = new GluePostProcessListener(
			metaRepository, 
			new SimpleExecutionEnvironment(environmentName, environment),
			serverPath
		);
		postprocessListener.setRefresh(EAIResourceRepository.isDevelopment());
		postprocessListener.setRealm(getRealm());
		EventSubscription<HTTPResponse, HTTPResponse> subscription = getDispatcher().subscribe(HTTPResponse.class, new EventHandler<HTTPResponse, HTTPResponse>() {
			@Override
			public HTTPResponse handle(HTTPResponse event) {
				try {
					// set it globally
					ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
					ServiceRuntime.getGlobalContext().put("service.context", getId());
					ServiceRuntime.getGlobalContext().put("service.source", "glue");
					return postprocessListener.handle(event);
				}
				catch (HTTPException e) {
					e.getContext().add(getId());
					throw e;
				}
				finally {
					// unset it
					ServiceRuntime.setGlobalContext(null);	
				}
			}
		});
		subscription.filter(HTTPServerUtils.limitToRequestPath(serverPath));
		return subscription;
	}
	
	public EventSubscription<HTTPRequest, HTTPEntity> registerPreProcessor(ScriptRepository repository) {
		try {
			return this.registerPreProcessor(repository, getEnvironmentProperties(), "root");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private EventSubscription<HTTPRequest, HTTPEntity> registerPreProcessor(ScriptRepository metaRepository, Map<String, String> environment, String environmentName) throws IOException {
		String serverPath = getServerPath();
		GluePreprocessListener preprocessListener = new GluePreprocessListener(
			authenticator,
			sessionProvider, 
			metaRepository, 
			new SimpleExecutionEnvironment(environmentName, environment),
			serverPath
		);
		preprocessListener.setRefresh(EAIResourceRepository.isDevelopment());
		preprocessListener.setTokenValidator(getTokenValidator());
		preprocessListener.setPermissionHandler(getPermissionHandler());
		preprocessListener.setRoleHandler(getRoleHandler());
		preprocessListener.setDeviceValidator(getDeviceValidator());
		preprocessListener.setRealm(getRealm());
		EventSubscription<HTTPRequest, HTTPEntity> subscription = getDispatcher().subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPEntity>() {
			@Override
			public HTTPEntity handle(HTTPRequest event) {
				try {
					// set it globally
					ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
					ServiceRuntime.getGlobalContext().put("service.context", getId());
					ServiceRuntime.getGlobalContext().put("service.source", "glue");
					return preprocessListener.handle(event);
				}
				catch (HTTPException e) {
					e.getContext().add(getId());
					throw e;
				}
				finally {
					// unset it
					ServiceRuntime.setGlobalContext(null);	
				}
			}
		});
		subscription.filter(HTTPServerUtils.limitToPath(getServerPath()));
		return subscription;
	}

	public boolean isSecure() {
		boolean secure = false;
		if (getConfig().getVirtualHost() != null && getConfig().getVirtualHost().getServer() != null) {
			secure = getConfig().getVirtualHost().getServer().isSecure();
		}
		return secure;
	}

	public Map<String, String> getEnvironmentProperties() throws IOException {
		boolean isDevelopment = EAIResourceRepository.isDevelopment();
		Map<String, String> environment = getProperties();
		if (isDevelopment) {
			environment.put("development", "true");
		}
		else {
			environment.put("development", "false");
		}
		// always set the id of the web artifact (need it to introspect artifact)
		String hostName = getConfiguration().getVirtualHost() == null ? null : getConfiguration().getVirtualHost().getConfiguration().getHost();
		
		Integer port = null;
		boolean secure = isSecure();
		
		if (getConfiguration().getVirtualHost() != null && getConfiguration().getVirtualHost().getServer() != null) {
			HTTPServerArtifact server = getConfiguration().getVirtualHost().getServer();
			port = server.getConfig().isProxied() ? server.getConfig().getProxyPort() : server.getConfiguration().getPort();
		}

		String url = null;
		String host = null;
		if (hostName != null) {
			if (port != null) {
				host = hostName + ":" + port;
			}
			else {
				host = hostName;
			}
			url = secure ? "https://" : "http://";
			url += host;
		}
		environment.put("realm", getRealm());
		environment.put("mobile", "false");
		environment.put("web", "true");
		environment.put("webApplicationId", getId());
		environment.put("secure", Boolean.toString(secure));
		environment.put("url", url);
		environment.put("host", host);
		environment.put("hostName", hostName);
		environment.put("version", getVersion());
		environment.put("cookiePath", getCookiePath());
		environment.put("serverPath", getServerPath());
		// whether or not it is optimized
		// when optimized, the resulting javascript etc is static between environments and should not leak data
		environment.put("optimized", Boolean.toString(getConfig().isOptimizedLoad()));
		return environment;
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
			String realm = getConfiguration().getRealm();
			if (realm == null) {
				Entry entry = getRepository().getEntry(getId());
				while (entry != null) {
					if (EAIRepositoryUtils.isProject(entry) && entry.getCollection() != null) {
						realm = entry.getCollection().getName();
						break;
					}
					entry = entry.getParent();
				}
			}
			if (realm == null) {
				realm = getId();
			}
			return realm;
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
	
	public PasswordAuthenticator getPasswordAuthenticator() {
		if (!passwordAuthenticatorResolved) {
			synchronized(this) {
				if (!passwordAuthenticatorResolved) {
					passwordAuthenticator = null;
					if (getConfig().getPasswordAuthenticationService() != null) {
						passwordAuthenticator = POJOUtils.newProxy(PasswordAuthenticator.class, wrap(getConfig().getPasswordAuthenticationService(), getMethod(PasswordAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
					}
					passwordAuthenticatorResolved = true;
				}
			}
		}
		return passwordAuthenticator;
	}
	
	public TypedAuthenticator getTypedAuthenticator() {
		if (!typedAuthenticatorResolved) {
			synchronized(this) {
				if (!typedAuthenticatorResolved) {
					typedAuthenticator = null;
					if (getConfig().getTypedAuthenticationService() != null) {
						typedAuthenticator = POJOUtils.newProxy(TypedAuthenticator.class, wrap(getConfig().getTypedAuthenticationService(), getMethod(TypedAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
					}
					typedAuthenticatorResolved = true;
				}
			}
		}
		return typedAuthenticator;
	}
	
	public SecretAuthenticator getSecretAuthenticator() {
		if (!secretAuthenticatorResolved) {
			synchronized(this) {
				if (!secretAuthenticatorResolved) {
					secretAuthenticator = null;
					if (getConfig().getSecretAuthenticationService() != null) {
						secretAuthenticator = POJOUtils.newProxy(SecretAuthenticator.class, wrap(getConfig().getSecretAuthenticationService(), getMethod(SecretAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
					}
					secretAuthenticatorResolved = true;
				}
			}
		}
		return secretAuthenticator;
	}
	
	@Override
	public Authenticator getAuthenticator() {
		if (!authenticatorResolved) {
			synchronized(this) {
				if (!authenticatorResolved) {
					try {
						PasswordAuthenticator passwordAuthenticator = null;
						if (getConfiguration().getPasswordAuthenticationService() != null) {
							passwordAuthenticator = POJOUtils.newProxy(PasswordAuthenticator.class, wrap(getConfiguration().getPasswordAuthenticationService(), getMethod(PasswordAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
						}
						SecretAuthenticator sharedSecretAuthenticator = null;
						if (getConfiguration().getSecretAuthenticationService() != null) {
							sharedSecretAuthenticator = POJOUtils.newProxy(SecretAuthenticator.class, wrap(getConfiguration().getSecretAuthenticationService(), getMethod(SecretAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
						}
						// we also want an authenticator if no password or shared secret is configured, for example only using the typed
//						if (passwordAuthenticator != null || sharedSecretAuthenticator != null) {
						authenticator = new WebApplicationAuthenticator(this, new CombinedAuthenticator(passwordAuthenticator, sharedSecretAuthenticator));
//						}
					}
					catch(IOException e) {
						throw new RuntimeException(e);
					}
					finally {
						authenticatorResolved = true;
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
	
	public Service wrap(Service service, Method method) {
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

	public Map<String, ComplexContent> getInputValues(Service service, Method method) {
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
					deviceValidator = getConfiguration().getDeviceValidatorService() != null 
						? POJOUtils.newProxy(
							DeviceValidator.class, 
							wrap(getConfiguration().getDeviceValidatorService(), getMethod(DeviceValidator.class, "isAllowed")), 
							getRepository(), 
							SystemPrincipal.ROOT
						) : null;
					deviceValidatorResolved = true;
				}
			}
		}
		return deviceValidator;
	}
	
	public RoleHandler getRoleHandler() throws IOException {
		if (!roleHandlerResolved) {
			synchronized(this) {
				if (!roleHandlerResolved) {
					if (getConfiguration().getRoleService() != null) {
						roleHandler = POJOUtils.newProxy(RoleHandler.class, wrap(getConfiguration().getRoleService(), getMethod(RoleHandler.class, "hasRole")), getRepository(), SystemPrincipal.ROOT);
					}
					roleHandlerResolved = true;
				}
			}
		}
		return roleHandler;
	}
	
	public PermissionHandler getPermissionHandler() throws IOException {
		if (!permissionHandlerResolved) {
			synchronized(this) {
				if (!permissionHandlerResolved) {
					if (getConfiguration().getPermissionService() != null) {
						permissionHandler = POJOUtils.newProxy(PermissionHandler.class, wrap(getConfiguration().getPermissionService(), getMethod(PermissionHandler.class, "hasPermission")), getRepository(), SystemPrincipal.ROOT);
					}
					permissionHandlerResolved = true;
				}
			}
		}
		return permissionHandler;
	}
	
	public PotentialPermissionHandler getPotentialPermissionHandler() throws IOException {
		if (!potentialPermissionHandlerResolved) {
			synchronized(this) {
				if (!potentialPermissionHandlerResolved) {
					if (getConfiguration().getPotentialPermissionService() != null) {
						potentialPermissionHandler = POJOUtils.newProxy(PotentialPermissionHandler.class, wrap(getConfiguration().getPotentialPermissionService(), getMethod(PotentialPermissionHandler.class, "hasPotentialPermission")), getRepository(), SystemPrincipal.ROOT);
					}
					potentialPermissionHandlerResolved = true;
				}
			}
		}
		return potentialPermissionHandler;
	}
	
	public RateLimitProvider getRateLimiter() throws IOException {
		if (!rateLimiterResolved) {
			synchronized(this) {
				if (!rateLimiterResolved) {
					rateLimiterResolved = true;
					DefinedService checker = getConfig().getRateLimitChecker();
					DefinedService settings = getConfig().getRateLimitSettings();
					DefinedService logger = getConfig().getRateLimitLogger();
					DefinedService rateLimiter = getConfig().getRateLimiter();
					if (rateLimiter != null) {
						Service rateLimiterWrap = rateLimiter == null ? null : wrap(rateLimiter, getMethod(RateLimitProvider.class, "rateLimit"));
						rateLimitProvider = POJOUtils.newProxy(RateLimitProvider.class,
							getRepository(),
							SystemPrincipal.ROOT,
							rateLimiterWrap
						);
					}
					// we need all these services to be of any use
					else if (checker != null && settings != null && logger != null) {
						Service checkerWrap = checker == null ? null : wrap(checker, getMethod(RateLimitProvider.class, "check"));
						Service settingsWrap = settings == null ? null : wrap(settings, getMethod(RateLimitProvider.class, "settings"));
						Service loggerWrap = logger == null ? null : wrap(logger, getMethod(RateLimitProvider.class, "log"));
						rateLimitProvider = POJOUtils.newProxy(RateLimitProvider.class,
							getRepository(),
							SystemPrincipal.ROOT,
							checkerWrap, 
							settingsWrap,
							loggerWrap
						);
					}
				}
			}
		}
		return rateLimitProvider;
	}
	
	public TokenValidator getTokenValidator() throws IOException {
		if (!tokenValidatorResolved) {
			synchronized(this) {
				if (!tokenValidatorResolved) {
					if (getConfiguration().getTokenValidatorService() != null) {
						tokenValidator = POJOUtils.newProxy(TokenValidator.class, wrap(getConfiguration().getTokenValidatorService(), getMethod(TokenValidator.class, "isValid")), getRepository(), SystemPrincipal.ROOT);
					}
					tokenValidatorResolved = true;
				}
			}
		}
		if (tokenValidator == null) {
			tokenValidator = new TimeoutTokenValidator();
		}
		return tokenValidator;
	}
	
	public UserLanguageProvider getUserLanguageProvider() throws IOException {
		if (!userLanguageProviderResolved) {
			synchronized(this) {
				if (!userLanguageProviderResolved) {
					if (getConfiguration().getLanguageProviderService() != null) {
						userLanguageProvider = POJOUtils.newProxy(UserLanguageProvider.class, wrap(getConfiguration().getLanguageProviderService(), getMethod(UserLanguageProvider.class, "getLanguage")), getRepository(), SystemPrincipal.ROOT);
					}
					userLanguageProviderResolved = true;
				}
			}
		}
		return userLanguageProvider;
	}
	
	public BearerAuthenticator getBearerAuthenticator() throws IOException {
		if (!bearerAuthenticatorResolved) {
			synchronized(this) {
				if (!bearerAuthenticatorResolved) {
					if (getConfiguration().getBearerAuthenticator() != null) {
						bearerAuthenticator = POJOUtils.newProxy(BearerAuthenticator.class, wrap(getConfiguration().getBearerAuthenticator(), getMethod(BearerAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
					}
					bearerAuthenticatorResolved = true;
				}
			}
		}
		return bearerAuthenticator;
	}
	
	public ArbitraryAuthenticator getArbitraryAuthenticator() throws IOException {
		if (!arbitraryAuthenticatorResolved) {
			synchronized(this) {
				if (!arbitraryAuthenticatorResolved) {
					if (getConfiguration().getArbitraryAuthenticator() != null) {
						arbitraryAuthenticator = POJOUtils.newProxy(ArbitraryAuthenticator.class, wrap(getConfiguration().getArbitraryAuthenticator(), getMethod(ArbitraryAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
					}
					arbitraryAuthenticatorResolved = true;
				}
			}
		}
		return arbitraryAuthenticator;
	}
	
	public TemporaryAuthenticator getTemporaryAuthenticator() throws IOException {
		if (!temporaryAuthenticatorResolved) {
			synchronized(this) {
				if (!temporaryAuthenticatorResolved) {
					if (getConfiguration().getTemporaryAuthenticator() != null) {
						temporaryAuthenticator = POJOUtils.newProxy(TemporaryAuthenticator.class, wrap(getConfiguration().getTemporaryAuthenticator(), getMethod(TemporaryAuthenticator.class, "authenticate")), getRepository(), SystemPrincipal.ROOT);
					}
					temporaryAuthenticatorResolved = true;
				}
			}
		}
		return temporaryAuthenticator;
	}
	
	public TemporaryAuthenticationGenerator getTemporaryAuthenticationGenerator() throws IOException {
		if (!temporaryAuthenticationGeneratorResolved) {
			synchronized(this) {
				if (!temporaryAuthenticationGeneratorResolved) {
					if (getConfiguration().getTemporaryAuthenticationGenerator() != null) {
						temporaryAuthenticationGenerator = POJOUtils.newProxy(TemporaryAuthenticationGenerator.class, wrap(getConfiguration().getTemporaryAuthenticationGenerator(), getMethod(TemporaryAuthenticationGenerator.class, "generate")), getRepository(), SystemPrincipal.ROOT);
					}
					temporaryAuthenticationGeneratorResolved = true;
				}
			}
		}
		return temporaryAuthenticationGenerator;
	}
	
	public TemporaryAuthenticationRevoker getTemporaryAuthenticationRevoker() throws IOException {
		if (!temporaryAuthenticationRevokerResolved) {
			synchronized(this) {
				if (!temporaryAuthenticationRevokerResolved) {
					if (getConfiguration().getTemporaryAuthenticationRevoker() != null) {
						temporaryAuthenticationRevoker = POJOUtils.newProxy(TemporaryAuthenticationRevoker.class, wrap(getConfiguration().getTemporaryAuthenticationRevoker(), getMethod(TemporaryAuthenticationRevoker.class, "revoke")), getRepository(), SystemPrincipal.ROOT);
					}
					temporaryAuthenticationRevokerResolved = true;
				}
			}
		}
		return temporaryAuthenticationRevoker;
	}
	
	public CORSHandler getCORSHandler() throws IOException {
		if (!corsHandlerResolved) {
			synchronized(this) {
				if (!corsHandlerResolved) {
					if (getConfiguration().getCorsChecker() != null) {
						corsHandler = POJOUtils.newProxy(CORSHandler.class, wrap(getConfiguration().getCorsChecker(), getMethod(CORSHandler.class, "check")), getRepository(), SystemPrincipal.ROOT);
					}
					corsHandlerResolved = true;
				}
			}
		}
		return corsHandler;
	}
	
	public RequestLanguageProvider getRequestLanguageProvider() throws IOException {
		if (!requestLanguageProviderResolved) {
			synchronized(this) {
				if (!requestLanguageProviderResolved) {
					if (getConfiguration().getRequestLanguageProviderService() != null) {
						requestLanguageProvider = POJOUtils.newProxy(RequestLanguageProvider.class, wrap(getConfiguration().getRequestLanguageProviderService(), getMethod(RequestLanguageProvider.class, "getLanguage")), getRepository(), SystemPrincipal.ROOT);
					}
					requestLanguageProviderResolved = true;
				}
			}
		}
		return requestLanguageProvider;
	}
	
	public Translator getTranslator() throws IOException {
		if (!translatorResolved) {
			synchronized(this) {
				if (!translatorResolved) {
					if (getConfig().getTranslationService() != null) {
						translator = POJOUtils.newProxy(Translator.class, wrap(getConfiguration().getTranslationService(), getMethod(Translator.class, "translate")), getRepository(), SystemPrincipal.ROOT);
					}
					translatorResolved = true;
				}
			}
		}
		return translator;
	}
	
	public SecretGenerator getSecretGenerator() {
		if (!secretGeneratorResolved) {
			synchronized(this) {
				if (!secretGeneratorResolved) {
					if (getConfig().getSecretGeneratorService() != null) {
						secretGenerator = POJOUtils.newProxy(SecretGenerator.class, wrap(getConfig().getSecretGeneratorService(), getMethod(SecretGenerator.class, "generate")), getRepository(), SystemPrincipal.ROOT);
					}
					secretGeneratorResolved = true;
				}
			}
		}
		return secretGenerator;
	}
	
	public LanguageProvider getLanguageProvider() {
		if (!languageProviderResolved) {
			synchronized(this) {
				if (!languageProviderResolved) {
					if (getConfig().getSupportedLanguagesService() != null) {
						languageProvider = POJOUtils.newProxy(LanguageProvider.class, wrap(getConfig().getSupportedLanguagesService(), getMethod(LanguageProvider.class, "getSupportedLanguages")), getRepository(), SystemPrincipal.ROOT);
					}
					languageProviderResolved = true;
				}
			}
		}
		return languageProvider;
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
		return getConfigurationFor(path, type, getFragmentConfigurations());
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
				// we also write the definition in a standardized format because
				// - we can't guarantee the definition is in the same project
				// - we can't guarantee the definition is a structure
				// we need the definition for merge scripts etc to know what is possible in the configuration rather than what is currently there
				if (content != null) {
					StructureManager.format(directory, content.getType(), "definition-" + typeId + ".xml", true);
				}
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

	@Override
	public List<RESTFragment> getFragments(boolean limitToUser, Token token) {
		if (restFragments == null) {
			synchronized(this) {
				if (restFragments == null) {
					List<RESTFragment> restFragments = new ArrayList<RESTFragment>();
					// add services first so they appear higher up in the resulting swagger
					if (getConfig().getServices() != null) {
						for (DefinedService service : getConfig().getServices()) {
							if (service == null) {
								continue;
							}
							restFragments.add(RESTServiceListener.asRestFragment(this, service));
						}
					}
					for (Script script : repository) {
						try {
							if (GlueListener.isPublicScript(script)) {
								if (script.getRoot() != null && script.getRoot().getContext() != null && script.getRoot().getContext().getAnnotations() != null && script.getRoot().getContext().getAnnotations().containsKey("path")) {
									String path = script.getRoot().getContext().getAnnotations().get("path");
									if (path != null && path.startsWith("/")) {
										path = path.substring(1);
									}
									String fullPath = ScriptUtils.getFullName(script).replace(".", "/") + (path == null ? "" : "/" + path);
									String operationId = script.getRoot().getContext().getAnnotations().get("operationId");
									String method = script.getRoot().getContext().getAnnotations().get("method");
									String input = script.getRoot().getContext().getAnnotations().get("input");
									String output = script.getRoot().getContext().getAnnotations().get("output");
									String consumes = script.getRoot().getContext().getAnnotations().get("consumes");
									String cache = script.getRoot().getContext().getAnnotations().get("cache");
									if (consumes == null) {
										consumes = "application/json, application/xml";
									}
									String produces = script.getRoot().getContext().getAnnotations().get("produces");
									if (produces == null) {
										produces = "application/json, application/xml";
									}
									
									if (operationId == null) {
										operationId = ScriptUtils.getFullName(script);
									}
									List<Element<?>> pathParameters = new ArrayList<Element<?>>();
									List<Element<?>> queryParameters = new ArrayList<Element<?>>();
									List<Element<?>> headerParameters = new ArrayList<Element<?>>();

									Type inputType = analyzeGlue(script.getRoot(), pathParameters, queryParameters, headerParameters);
									
									// if you explicitly set an input type, that wins
									if (input != null) {
										inputType = DefinedTypeResolverFactory.getInstance().getResolver().resolve(input);
										if (inputType == null) {
											throw new RuntimeException("Could not resolve input type: " + input);
										}
									}
									
									Type outputType = null;
									if (output != null) {
										outputType = SimpleTypeWrapperFactory.getInstance().getWrapper().getByName(output);
										if (outputType == null) {
											outputType = DefinedTypeResolverFactory.getInstance().getResolver().resolve(output);
										}
										if (outputType == null) {
											throw new RuntimeException("Could not resolve output type: " + output);
										}
									}
									
									if (method == null) {
										method = inputType == null ? "GET" : "POST";
									}
									else {
										method = method.toUpperCase();
									}
									
									String description = script.getRoot().getContext().getDescription();
									String title = script.getRoot().getContext().getAnnotations().get("title");
									String tags = script.getRoot().getContext().getAnnotations().get("tags");
									String roles = script.getRoot().getContext().getAnnotations().get("role");
									String permissionAction = script.getRoot().getContext().getAnnotations().get("action");
									String permissionContext = script.getRoot().getContext().getAnnotations().get("context");
									
									String rateLimitAction = script.getRoot().getContext().getAnnotations().get("limit");
									
									Documented documentation = null;
									if (description != null || title != null || tags != null) {
										documentation = new Documented() {
											@Override
											public String getTitle() {
												return title;
											}
											@Override
											public String getDescription() {
												return description;
											}
											@Override
											public Collection<String> getTags() {
												return tags == null ? null : Arrays.asList(tags.split("[\\s]*,[\\s]*"));
											}
											@Override
											public String getMimeType() {
												return "text/x-markdown";
											}
										};
									}
									
									restFragments.add(newFragment(
										operationId, 
										fullPath, 
										method, 
										Arrays.asList(consumes.split("[\\s]*,[\\s]*")), 
										Arrays.asList(produces.split("[\\s]*,[\\s]*")), 
										inputType, 
										outputType, 
										queryParameters, 
										headerParameters, 
										pathParameters,
										documentation,
										roles == null || roles.isEmpty() ? null : Arrays.asList(roles.split("[\\s]*,[\\s]*")),
										permissionAction,
										permissionContext,
										cache != null,
										rateLimitAction == null ? operationId : rateLimitAction,
										null
									));
								}
							}
						}
						catch (Exception e) {
							logger.warn("Could not analyze rest services in script: " + ScriptUtils.getFullName(script), e);
						}
					}
					this.restFragments = restFragments;
				}
			}
		}
		return restFragments;
	}
	
	private RESTFragment newFragment(String id, String path, String method, List<String> consumes, List<String> produces, Type input, Type output, List<Element<?>> query, List<Element<?>> header, List<Element<?>> paths, Documented documentation, List<String> allowedRoles, String permissionAction, String permissionContext, boolean isCacheable, String rateLimitAction, String rateLimitContext) {
		return new RESTFragment() {
			@Override
			public String getId() {
				return id;
			}
			@Override
			public String getPath() {
				return path;
			}
			@Override
			public String getMethod() {
				return method;
			}
			@Override
			public List<String> getConsumes() {
				return consumes;
			}
			@Override
			public List<String> getProduces() {
				return produces;
			}
			@Override
			public Type getInput() {
				return input;
			}
			@Override
			public Type getOutput() {
				return output;
			}
			@Override
			public List<Element<?>> getQueryParameters() {
				return query;
			}
			@Override
			public List<Element<?>> getHeaderParameters() {
				return header;
			}
			@Override
			public List<Element<?>> getPathParameters() {
				return paths;
			}
			public Documented getDocumentation() {
				return documentation;
			}
			@Override
			public List<String> getAllowedRoles() {
				return allowedRoles;
			}
			@Override
			public String getPermissionAction() {
				return permissionAction;
			}
			@Override
			public String getPermissionContext() {
				return permissionContext;
			}
			@Override
			public boolean isCacheable() {
				return isCacheable;
			}
			@Override
			public String getRateLimitAction() {
				return rateLimitAction;
			}
			@Override
			public String getRateLimitContext() {
				return rateLimitContext;
			}
		};
	}
	
	private Type analyzeGlue(ExecutorGroup group, List<Element<?>> pathParameters, List<Element<?>> queryParameters, List<Element<?>> headerParameters) {
		Type body = null;
		for (Executor executor : group.getChildren()) {
			if (executor instanceof AssignmentExecutor && !(((AssignmentExecutor) executor).isOverwriteIfExists()) && executor.getContext() != null && executor.getContext().getAnnotations() != null) {
				Map<String, String> annotations = executor.getContext().getAnnotations();
				if (annotations.containsKey("get")) {
					String name = annotations.get("get");
					if (name == null) {
						name = ((AssignmentExecutor) executor).getVariableName();
					}
					if (name != null) {
						queryParameters.add(resolveElement(name, (AssignmentExecutor) executor, true));
					}
				}
				else if (annotations.containsKey("path")) {
					String name = annotations.get("path");
					if (name == null) {
						name = ((AssignmentExecutor) executor).getVariableName();
					}
					if (name != null) {
						pathParameters.add(resolveElement(name, (AssignmentExecutor) executor, true));
					}
				}
				else if (annotations.containsKey("header")) {
					String name = annotations.get("header");
					if (name == null) {
						name = ((AssignmentExecutor) executor).getVariableName();
					}
					if (name != null) {
						headerParameters.add(resolveElement(name, (AssignmentExecutor) executor, true));
					}
				}
				// TODO: check for content, if there is any, check the type, if there is any use that, otherwise we set byte[] and octet stream!
				else if (annotations.containsKey("content")) {
					String optionalType = ((AssignmentExecutor) executor).getOptionalType();
					if (body == null || body instanceof SimpleType) {
						if (optionalType != null) {
							body = resolveType(optionalType, false);
						}
						else {
							body = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(byte[].class);
						}
					}
				}
			}
			if (executor instanceof ExecutorGroup) {
				Type analyzed = analyzeGlue((ExecutorGroup) executor, pathParameters, queryParameters, headerParameters);
				if (body == null) {
					body = analyzed;
				}
			}
		}
		return body;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Element<?> resolveElement(String name, AssignmentExecutor executor, boolean shouldBeSimple) {
		Type type = resolveType(executor.getOptionalType(), shouldBeSimple);
		List<Value<?>> values = new ArrayList<Value<?>>();
		if (executor.isList()) {
			values.add(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0));
		}
		String nullable = executor.getContext().getAnnotations().get("null");
		if (nullable == null || nullable.trim().isEmpty()) {
			nullable = "true";
		}
		if (Boolean.parseBoolean(nullable)) {
			values.add(new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0));
		}
		return type instanceof ComplexType
			? new ComplexElementImpl(name, (ComplexType) type, null, values.toArray(new Value[0]))
			: new SimpleElementImpl(name, (SimpleType) type, null, values.toArray(new Value[0]));
	}
	
	private Type resolveType(String typeString, boolean shouldBeSimple) {
		// any object
		if (typeString == null) {
			typeString = shouldBeSimple ? String.class.getName() : Object.class.getName();
		}
		else {
			Class<?> wrapDefault = DefaultOptionalTypeProvider.wrapDefault(typeString);
			if (wrapDefault != null) {
				typeString = wrapDefault.getName();
			}
		}
		Type type = DefinedTypeResolverFactory.getInstance().getResolver().resolve(typeString);
		if (type == null && repository != null) {
			try {
				Script script = repository.getScript(typeString);
				if (script != null) {
					type = GlueTypeUtils.toType(ScriptUtils.getFullName(script), ScriptUtils.getInputs(script), new StructureGenerator(), repository, DefinedTypeResolverFactory.getInstance().getResolver());
				}
			}
			catch (Exception e) {
				throw new RuntimeException("Could not parse script: " + typeString);
			}
		}
		return type;
	}

	public ServiceMethodProvider getServiceMethodProvider() {
		if (serviceMethodProvider == null) {
			serviceMethodProvider = new ServiceMethodProvider(getRepository(), getRepository());
		}
		return serviceMethodProvider;
	}
	
	public GlueParserProvider getParserProvider() {
		if (parserProvider == null) {
			parserProvider = new GlueWebParserProvider(getServiceMethodProvider(), new StaticJavaMethodProvider(new WebApplicationMethods(this))); 
		}
		return parserProvider;
	}
	
	@SuppressWarnings("unchecked")
	public void putConfiguration(Object object, String path, Boolean environmentSpecific) {
		Map<String, Map<String, ComplexContent>> fragmentConfigurations = getFragmentConfigurations();
		if (!(object instanceof ComplexContent)) {
			object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
		}
		String id = ((DefinedType) ((ComplexContent) object).getType()).getId();
		synchronized(this) {
			if (!fragmentConfigurations.containsKey(id)) {
				fragmentConfigurations.put(id, new HashMap<String, ComplexContent>());
			}
			// if we did not set a preference, try to deduce what the current setting is
			// note that we might be dealing with another instance of the object, so we can't just do it by reference
			if (environmentSpecific == null && fragmentConfigurations.get(id).containsKey(path)) {
				ComplexContent complexContent = fragmentConfigurations.get(id).get(path);
				environmentSpecific = environmentSpecificConfigurations.contains(complexContent);
			}
			fragmentConfigurations.get(id).put(path, (ComplexContent) object);
			if (environmentSpecific != null) {
				if (environmentSpecific) {
					environmentSpecificConfigurations.add((ComplexContent) object);
				}
				else {
					environmentSpecificConfigurations.remove((ComplexContent) object);
				}
			}
		}
	}

	public List<RobotEntry> getRobotEntries() {
		return robotEntries;
	}

	public void setRobotEntries(List<RobotEntry> robotEntries) {
		this.robotEntries = robotEntries;
	}

	private String getRobotsContent() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (RobotEntry entry : robotEntries) {
			if (first) {
				first = false;
			}
			else {
				builder.append("\n");
			}
			builder.append(entry.getKey());
			if (entry.getValue() != null) {
				builder.append(": ").append(entry.getValue());
			}
		}
		return builder.toString();
	}

	@Override
	public List<Feature> getAvailableFeatures() {
		if (availableFeatures == null || EAIResourceRepository.isDevelopment()) {
			synchronized(this) {
				if (availableFeatures == null || EAIResourceRepository.isDevelopment()) {
					Map<String, Feature> features = new HashMap<String, Feature>();
					try {
						features(getDirectory(), true, features);
					}
					catch (IOException e) {
						logger.error("Could not list features", e);
					}
					// recursively check includes, but only in web components and the like
					// we only want to send features along that exist in javascript or the like, that get streamed to the frontend
					// not features in for example rest services, they get picked up separately and should _not_ be sent to the frontend unless they are explicitly used there as well
					if (getConfig().getWebFragments() != null) {
						for (WebFragment fragment : getConfig().getWebFragments()) {
							if (fragment instanceof FeaturedWebArtifact) {
								List<Feature> childFeatures = ((FeaturedWebArtifact) fragment).getAvailableWebFeatures();
								if (childFeatures != null) {
									for (Feature feature : childFeatures) {
										features.put(feature.getName(), feature);
									}
								}
							}
						}
					}
					availableFeatures = new ArrayList<Feature>(features.values());
				}
			}
		}
		return availableFeatures;
	}
	
	private void features(ResourceContainer<?> container, boolean recursive, Map<String, Feature> features) throws IOException {
		if (container != null) {
			for (Resource resource : container) {
				if (resource instanceof ReadableResource && resource.getName().matches(".*\\.(tpl|js|css|gcss|glue|json)")) {
					ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
					try {
						byte[] bytes = IOUtils.toBytes(readable);
						String source = new String(bytes, "UTF-8");
						for (String key : ImperativeSubstitutor.getValues("@", source)) {
							String category = null;
							if (key.matches("(?s)^(?:.*?::|[a-zA-Z0-9.]+:).*")) {
								category = key.replaceAll("(?s)^(?:(.*?)::|([a-zA-Z0-9.]+):).*", "$1$2");
								key = key.replaceAll("(?s)^(?:.*?::|[a-zA-Z0-9.]+:)(.*)", "$1");
							}
							String unique = category + "::" + key;
							if (!features.containsKey(unique)) {
								features.put(unique, new FeatureImpl(category != null ? category : key, category != null ? key : null));
							}
						}
					}
					finally {
						readable.close();
					}
				}
				if (recursive && resource instanceof ResourceContainer) {
					features((ResourceContainer<?>) resource, recursive, features);
				}
			}
		}
	}
	
	public boolean canTestFeature(Token token, String featureName) {
		try {
			boolean isAllowed = false;
			RoleHandler roleHandler = this.getRoleHandler();
			if (roleHandler != null && getConfig().getTestRole() != null && !getConfig().getTestRole().isEmpty()) {
				for (String role : getConfig().getTestRole()) {
					if (roleHandler.hasRole(token, role)) {
						isAllowed = true;
						break;
					}
				}
			}
			// check permission handler as well
			if (!isAllowed) {
				PermissionHandler permissionHandler = this.getPermissionHandler();
				isAllowed = permissionHandler != null && permissionHandler.hasPermission(token, "context:" + getId(), "application.test");
//				if (!isAllowed) {
//					isAllowed = permissionHandler != null && permissionHandler.hasPermission(token, "context:" + getId(), "application.test." + featureName);
//				}
			}
			return isAllowed;
		}
		catch (Exception e) {
			logger.error("Can not check feature testing", e);
			return false;
		}
	}
	
	public WebFragmentProvider getApiFragmentProvider() {
		List<WebFragment> fragments = getConfig().getWebFragments();
		if (fragments != null) {
			for (WebFragment fragment : fragments) {
				if (fragment instanceof WebFragmentProvider) {
					String path = ((WebFragmentProvider) fragment).getRelativePath();
					// if we have "api" in the path, we assume it is the api artifact
					if (path != null && path.contains("/api/")) {
						return (WebFragmentProvider) fragment;
					}
				}
			}
		}
		return null;
	}
	
	public String getServicePath() {
		try {
			WebFragmentProvider api = getApiFragmentProvider();
			String result = api == null ? null : api.getRelativePath();
			// it is possible that we _are_ an API application and that we have it in our path
			// at that point, the _relative_ url does not need anything added
			if (result == null && getServerPath().contains("/api/")) {
				result = "/";
			}
			// the default for an internal API
			if (result == null) {
				result = "/api/otr";
			}
			return result;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public InputStream encrypt(InputStream input) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, InvalidParameterSpecException {
		byte[] bytes = IOUtils.toBytes(IOUtils.wrap(input));
		if (getConfig().getJwtKeyAlias() != null && getConfig().getJwtKeyStore() != null) {
			KeyStoreArtifact keystore = getConfig().getJwtKeyStore();
			try {
				SecretKey secretKey = keystore.getKeyStore().getSecretKey(getConfig().getJwtKeyAlias());
				return processEncryption(input, secretKey, Cipher.ENCRYPT_MODE);
			}
			catch (Exception e) {
				try {
					PrivateKey privateKey = keystore.getKeyStore().getPrivateKey(getConfig().getJwtKeyAlias());
					return processEncryption(input, privateKey, Cipher.ENCRYPT_MODE);
				}
				catch (Exception f) {
					throw new IllegalStateException("A keystore and alias are configured but do not point to a valid key for application: " + getId());
				}
			}
		}
		return new ByteArrayInputStream(SecurityUtils.pbeEncrypt(bytes, getId(), PBEAlgorithm.AES256, true).getBytes("ASCII"));
	}
	
	private InputStream processEncryption(InputStream input, Key secretKey, Integer mode) throws KeyStoreException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(secretKey.getAlgorithm());
		cipher.init(mode, secretKey);
		int read = 0;
		byte [] buffer = new byte[8092];
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		while ((read = input.read(buffer)) >= 0) {
			output.write(cipher.update(buffer, 0, read));
		}
		output.write(cipher.doFinal());
		return new ByteArrayInputStream(output.toByteArray());
	}
	
	public InputStream decrypt(InputStream input) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		byte[] bytes = IOUtils.toBytes(IOUtils.wrap(input));
		if (getConfig().getJwtKeyAlias() != null && getConfig().getJwtKeyStore() != null) {
			KeyStoreArtifact keystore = getConfig().getJwtKeyStore();
			try {
				SecretKey secretKey = keystore.getKeyStore().getSecretKey(getConfig().getJwtKeyAlias());
				return processEncryption(input, secretKey, Cipher.DECRYPT_MODE);
			}
			catch (Exception e) {
				try {
					PrivateKey privateKey = keystore.getKeyStore().getPrivateKey(getConfig().getJwtKeyAlias());
					return processEncryption(input, privateKey, Cipher.DECRYPT_MODE);
				}
				catch (Exception f) {
					throw new IllegalStateException("A keystore and alias are configured but do not point to a valid key for application: " + getId());
				}
			}
		}
		return new ByteArrayInputStream(SecurityUtils.pbeDecrypt(new String(bytes, "ASCII"), getId(), PBEAlgorithm.AES256, true));
	}

	@Override
	public void beforeCommit() {
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PRIVATE);
		ResourceContainer<?> cacheFolder = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("cache");
		// if we have a cache folder, we look for javascript and css and compress it
		if (cacheFolder != null) {
			// we probably need to delete some children as well, so let's not work on the iterator directly
			List<Resource> children = new ArrayList<Resource>();
			for (Resource child : cacheFolder) {
				children.add(child);
			}
			for (Resource child : children) {
				try {
					Charset charset = Charset.forName("UTF-8");
					// if we have a javascript that might need compilation, let's do that!
					if (child instanceof ReadableResource && child.getName().endsWith(".js") && !child.getName().endsWith(".compiled.js")) {
						// check if there is already a compiled one, let's remove it
						// if our compilation fails, we don't want it to be mistakingly used
						String compiledName = child.getName().replaceAll("\\.js$", ".compiled.js");
						Resource compiledChild = cacheFolder.getChild(compiledName);
						if (compiledChild != null) {
							((ManageableContainer<?>) cacheFolder).delete(compiledName);
						}
						
						CompilerOptions options = new CompilerOptions();
						CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
	//						CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
						boolean allowEs6 = true;
						if (allowEs6) {
							options.setLanguageIn(LanguageMode.ECMASCRIPT_2021);	// ECMASCRIPT6_TYPED
						}
						else {
							options.setLanguageIn(LanguageMode.ECMASCRIPT5);
						}
						// set output mode
	//					options.setLanguageOut(LanguageMode.ECMASCRIPT3);
							
						try (ReadableContainer<ByteBuffer> readable = ((ReadableResource) child).getReadable()) {
							byte [] originalContent = IOUtils.toBytes(readable);
							
							SourceFile file = SourceFile.fromCode("file.js", new String(originalContent, charset));
							SourceFile external = SourceFile.fromCode("file2.js", "");
							
							com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();
							compiler.compile(external, file, options);
		
							String source = compiler.toSource();
							byte [] compressedContent = source.getBytes(charset);
							
							compiledChild = ((ManageableContainer<?>) cacheFolder).create(compiledName, "application/javascript");
							
							try (WritableContainer<ByteBuffer> writable = ((WritableResource) compiledChild).getWritable()) {
								writable.write(IOUtils.wrap(compressedContent, true));
							}
						}
					}
					else if (child instanceof ReadableResource && child.getName().endsWith(".css") && !child.getName().endsWith(".compiled.css")) {
						// check if there is already a compiled one, let's remove it
						// if our compilation fails, we don't want it to be mistakingly used
						String compiledName = child.getName().replaceAll("\\.css$", ".compiled.css");
						Resource compiledChild = cacheFolder.getChild(compiledName);
						if (compiledChild != null) {
							((ManageableContainer<?>) cacheFolder).delete(compiledName);
						}
						try (ReadableContainer<ByteBuffer> readable = ((ReadableResource) child).getReadable()) {
							byte[] originalContent = IOUtils.toBytes(readable);
							
							String css = new String(originalContent, charset);
							// strip comments
							css = css.replaceAll("(?s)/\\*.*?\\*/", "");
							// strip all whitespace
							css = css.replaceAll("(\\{|:|;|\\}|,)[\\s]+", "$1");
							byte [] compressedContent = css.getBytes(charset);
							
							compiledChild = ((ManageableContainer<?>) cacheFolder).create(compiledName, "text/css");
							
							try (WritableContainer<ByteBuffer> writable = ((WritableResource) compiledChild).getWritable()) {
								writable.write(IOUtils.wrap(compressedContent, true));
							}
						}
					}
					else if (child instanceof ReadableResource && child.getName().endsWith(".html") && !child.getName().endsWith(".compiled.html")) {
						// check if there is already a compiled one, let's remove it
						// if our compilation fails, we don't want it to be mistakingly used
						String compiledName = child.getName().replaceAll("\\.html$", ".compiled.html");
						Resource compiledChild = cacheFolder.getChild(compiledName);
						if (compiledChild != null) {
							((ManageableContainer<?>) cacheFolder).delete(compiledName);
						}
						try (ReadableContainer<ByteBuffer> readable = ((ReadableResource) child).getReadable()) {
							byte[] originalContent = IOUtils.toBytes(readable);
							
							String html = new String(originalContent, charset);
							
							// remove html comments
							html = html.replaceAll("(?s)<!--.*?-->", "");
							
							// remove whitespace between tags
							html = html.replaceAll("(?s)>[\\s]+<", "><");
							
							// remove all additional whitespace
							html = html.replaceAll("(?s)[\\s]+", " ");
							
							byte [] compressedContent = html.getBytes(charset);
							compiledChild = ((ManageableContainer<?>) cacheFolder).create(compiledName, "text/html");
							try (WritableContainer<ByteBuffer> writable = ((WritableResource) compiledChild).getWritable()) {
								writable.write(IOUtils.wrap(compressedContent, true));
							}
						}
					}
				}
				catch (IOException e) {
					logger.warn("Could not compile javascript content", e);
				}
			}
		}
	}

	public Date getLastCacheUpdate() {
		if (EAIResourceRepository.isDevelopment()) {
			return null;
		}
		if (lastCacheUpdate == null) {
			ResourceContainer<?> privateDirectory = (ResourceContainer<?>) getDirectory().getChild(EAIResourceRepository.PRIVATE);
			ResourceContainer<?> cacheFolder = privateDirectory == null ? null : (ResourceContainer<?>) privateDirectory.getChild("cache");
			if (cacheFolder != null) {
				Date latest = null;
				for (Resource child : cacheFolder) {
					if (child instanceof TimestampedResource) {
						Date lastModified = ((TimestampedResource) child).getLastModified();
						if (lastModified != null && (latest == null || lastModified.after(latest))) {
							latest = lastModified;
						}
					}
				}
				lastCacheUpdate = latest;
			}
		}
		return lastCacheUpdate;
	}

	// TODO: we want service level agreement managers where you can finetune all this
	// but for now, we just set a 2s warning limit on all calls
	// anything longer than that should probably be reported
	@Override
	public ServiceLevelAgreement getAgreementFor(Service service) {
		return service instanceof WebFragment ? new ServiceLevelAgreement() {
			@Override
			public Long getThresholdDuration() {
				return 2l * 60 * 1000;
			}
			@Override
			public EventSeverity getSeverity() {
				return EventSeverity.WARNING;
			}
			@Override
			public boolean isExplicit() {
				return false;
			}
		} : null; 
	}
	
}
