package be.nabu.eai.module.web.application;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.api.CacheProviderArtifact;
import be.nabu.eai.repository.api.ListableSinkProviderArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "webApplication")
@XmlType(propOrder = { "virtualHost", "realm", "path", "cookiePath", "charset", "allowBasicAuthentication", "failedLoginThreshold", "failedLoginWindow", "failedLoginBlacklistDuration", "passwordAuthenticationService", "secretAuthenticationService", "bearerAuthenticator", "temporaryAuthenticator", "temporaryAuthenticationGenerator", "permissionService", "potentialPermissionService", "roleService", "tokenValidatorService", "deviceValidatorService", "translationService", "supportedLanguagesService", "languageProviderService", "requestLanguageProviderService", "defaultLanguage", "rateLimiter", "rateLimiterDatabase", "requestSubscriber", "whitelistedCodes", "sessionCacheProvider", "sessionCacheId", "maxTotalSessionSize", "maxSessionSize", "sessionTimeout", "sessionProviderApplication", "scriptCacheProvider", "maxTotalScriptCacheSize", "maxScriptCacheSize", "scriptCacheTimeout", "addCacheHeaders", "jwtKeyStore", "jwtKeyAlias", "allowJwtBearer", "allowContentEncoding", "webFragments", "html5Mode", "forceRequestLanguage", "proxyPath", "ignoreLanguageCookie" })
public class WebApplicationConfiguration {

	// the id of the cache used by this webapplication, this allows for example sessions to be shared cross web application
	private String sessionCacheId;
	private CacheProviderArtifact sessionCacheProvider, scriptCacheProvider;
	private Long maxTotalSessionSize, maxSessionSize, sessionTimeout, maxTotalScriptCacheSize, maxScriptCacheSize, scriptCacheTimeout;
	private VirtualHostArtifact virtualHost;
	private String path, cookiePath;
	private String charset;
	private String realm;
	private String whitelistedCodes;
	private Long failedLoginThreshold, failedLoginWindow, failedLoginBlacklistDuration;
	private String defaultLanguage;
	private boolean addCacheHeaders = true;
	private boolean ignoreLanguageCookie;
	
	// we assume the proxy strips the path, but to build correct links for the outside world, we need to know this
	private String proxyPath;
	
	private DefinedService passwordAuthenticationService, secretAuthenticationService;
	private DefinedService permissionService, potentialPermissionService;
	private DefinedService roleService;
	private DefinedService tokenValidatorService;
	private DefinedService translationService, supportedLanguagesService;
	private DefinedService languageProviderService, requestLanguageProviderService;
	private DefinedService deviceValidatorService;
	private DefinedService requestSubscriber;
	private DefinedService bearerAuthenticator;
	private DefinedService temporaryAuthenticator, temporaryAuthenticationGenerator;
	private Boolean allowBasicAuthentication;
	private List<WebFragment> webFragments;
	private ListableSinkProviderArtifact rateLimiterDatabase;
	// you can reuse the sessions from another application
	private WebApplication sessionProviderApplication;
	
	private String jwtKeyAlias;
	private KeyStoreArtifact jwtKeyStore;
	private boolean allowJwtBearer, allowContentEncoding = true, html5Mode;
	private boolean forceRequestLanguage;
	
	private DefinedService rateLimiter;
	
	@Comment(title = "The path that the web application will listen to", description = "Multiple web applications can be hosted on a single host as long as they have different root paths")
	@EnvironmentSpecific
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	@Advanced
	@Comment(title = "The encoding used for the response content")
	public String getCharset() {
		return charset;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Comment(title = "This service is responsible for translating content in the web application", description = "Use the syntax %{The sentence} to encapsulate content that has to be translated")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.Translator.translate")
	public DefinedService getTranslationService() {
		return translationService;
	}
	public void setTranslationService(DefinedService translationService) {
		this.translationService = translationService;
	}
	
	@Comment(title = "List all of the available languages")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.LanguageProvider.getSupportedLanguages")
	public DefinedService getSupportedLanguagesService() {
		return supportedLanguagesService;
	}
	public void setSupportedLanguagesService(DefinedService supportedLanguagesService) {
		this.supportedLanguagesService = supportedLanguagesService;
	}
	
	@Comment(title = "This service is responsible for indicating which language a user wants based on e.g. profile settings")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.UserLanguageProvider.getLanguage")	
	public DefinedService getLanguageProviderService() {
		return languageProviderService;
	}
	public void setLanguageProviderService(DefinedService languageProviderService) {
		this.languageProviderService = languageProviderService;
	}
	
	@Comment(title = "This service is responsible for indicating which language a user wants based on the request")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RequestLanguageProvider.getLanguage")	
	public DefinedService getRequestLanguageProviderService() {
		return requestLanguageProviderService;
	}
	public void setRequestLanguageProviderService(DefinedService requestLanguageProviderService) {
		this.requestLanguageProviderService = requestLanguageProviderService;
	}
	
	@Comment(title = "This service is responsible for authenticating a username with a given password", description = "If you return a secret in the response of this service, the secret authenticator can be used to remember users automatically")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.PasswordAuthenticator.authenticate")
	public DefinedService getPasswordAuthenticationService() {
		return passwordAuthenticationService;
	}
	public void setPasswordAuthenticationService(DefinedService passwordAuthenticationService) {
		this.passwordAuthenticationService = passwordAuthenticationService;
	}

	@Comment(title = "This service is responsible for creating a valid token from a bearer authentication")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.BearerAuthenticator.authenticate")
	public DefinedService getBearerAuthenticator() {
		return bearerAuthenticator;
	}
	public void setBearerAuthenticator(DefinedService bearerAuthenticator) {
		this.bearerAuthenticator = bearerAuthenticator;
	}
	
	@Comment(title = "This service is responsible for remembering a previously logged in user based on a shared secret")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.SecretAuthenticator.authenticate")
	public DefinedService getSecretAuthenticationService() {
		return secretAuthenticationService;
	}
	public void setSecretAuthenticationService(DefinedService secretAuthenticationService) {
		this.secretAuthenticationService = secretAuthenticationService;
	}
	
	@Comment(title = "This service is reponsible for temporarily authenticating someone to perform a certain action")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.TemporaryAuthenticator.authenticate")
	public DefinedService getTemporaryAuthenticator() {
		return temporaryAuthenticator;
	}
	public void setTemporaryAuthenticator(DefinedService temporaryAuthenticator) {
		this.temporaryAuthenticator = temporaryAuthenticator;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.TemporaryAuthenticationGenerator.generate")
	public DefinedService getTemporaryAuthenticationGenerator() {
		return temporaryAuthenticationGenerator;
	}
	public void setTemporaryAuthenticationGenerator(DefinedService temporaryAuthenticationGenerator) {
		this.temporaryAuthenticationGenerator = temporaryAuthenticationGenerator;
	}
	
	@Comment(title = "This service is responsible for checking if a user has a specific permission", description = "A permission is the combination of an action (what are you trying to do) in an optional context (e.g. an action on an entity)")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.PermissionHandler.hasPermission")
	public DefinedService getPermissionService() {
		return permissionService;
	}
	public void setPermissionService(DefinedService permissionService) {
		this.permissionService = permissionService;
	}

	@Comment(title = "This service is responsible for checking if a user has a specific role")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.RoleHandler.hasRole")
	public DefinedService getRoleService() {
		return roleService;
	}
	public void setRoleService(DefinedService roleService) {
		this.roleService = roleService;
	}

	@Comment(title = "This service is responsible for checking if a user potentially has a permission regardless of the context. Once a context is known, the permission handler might still not grant the permission though.")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.PotentialPermissionHandler.hasPotentialPermission")
	public DefinedService getPotentialPermissionService() {
		return potentialPermissionService;
	}
	public void setPotentialPermissionService(DefinedService potentialPermissionService) {
		this.potentialPermissionService = potentialPermissionService;
	}
	
	@Advanced
	@Comment(title = "This service is responsible for checking if a previously granted token is still valid")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.TokenValidator.isValid")
	public DefinedService getTokenValidatorService() {
		return tokenValidatorService;
	}
	public void setTokenValidatorService(DefinedService tokenValidatorService) {
		this.tokenValidatorService = tokenValidatorService;
	}

	@Comment(title = "This service is responsible for checking if a certain device is allowed for a given user")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.DeviceValidator.isAllowed")	
	public DefinedService getDeviceValidatorService() {
		return deviceValidatorService;
	}
	public void setDeviceValidatorService(DefinedService deviceValidatorService) {
		this.deviceValidatorService = deviceValidatorService;
	}
	
	@Advanced
	public Boolean getAllowBasicAuthentication() {
		return allowBasicAuthentication;
	}
	public void setAllowBasicAuthentication(Boolean allowBasicAuthentication) {
		this.allowBasicAuthentication = allowBasicAuthentication;
	}

	public String getRealm() {
		return realm;
	}
	public void setRealm(String realm) {
		this.realm = realm;
	}

	@Comment(title = "A comma separated list of error codes that are sent back to the frontend to given additional feedback on what went wrong", description = "By default all errors are simply returned as an HTTP error code with no additional information, whitelisting an error code gives more context to the frontend, allowing for specific error handling")
	public String getWhitelistedCodes() {
		return whitelistedCodes;
	}
	public void setWhitelistedCodes(String whitelistedCodes) {
		this.whitelistedCodes = whitelistedCodes;
	}

	@Advanced
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public CacheProviderArtifact getSessionCacheProvider() {
		return sessionCacheProvider;
	}
	public void setSessionCacheProvider(CacheProviderArtifact sessionCacheProvider) {
		this.sessionCacheProvider = sessionCacheProvider;
	}
	
	@Comment(title = "The script cache provider is used to temporarily cache page results, _always_ use a serializing cache for this and _never_ cache a page with user-specific content on it!", description = "You can set an '@cache <timeout>' at the top of a script to enable caching. The timeout is optional and can be set to 0 for indefinite cached values.")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public CacheProviderArtifact getScriptCacheProvider() {
		return scriptCacheProvider;
	}
	public void setScriptCacheProvider(CacheProviderArtifact scriptCacheProvider) {
		this.scriptCacheProvider = scriptCacheProvider;
	}
	
	@Advanced
	public Long getMaxTotalSessionSize() {
		return maxTotalSessionSize;
	}
	public void setMaxTotalSessionSize(Long maxTotalSessionSize) {
		this.maxTotalSessionSize = maxTotalSessionSize;
	}

	@Advanced
	public Long getMaxSessionSize() {
		return maxSessionSize;
	}
	public void setMaxSessionSize(Long maxSessionSize) {
		this.maxSessionSize = maxSessionSize;
	}

	@Advanced
	public Long getSessionTimeout() {
		return sessionTimeout;
	}
	public void setSessionTimeout(Long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<WebFragment> getWebFragments() {
		return webFragments;
	}
	public void setWebFragments(List<WebFragment> webFragments) {
		this.webFragments = webFragments;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public VirtualHostArtifact getVirtualHost() {
		return virtualHost;
	}
	public void setVirtualHost(VirtualHostArtifact virtualHost) {
		this.virtualHost = virtualHost;
	}
	
	@Advanced
	public Long getFailedLoginThreshold() {
		return failedLoginThreshold;
	}
	public void setFailedLoginThreshold(Long failedLoginThreshold) {
		this.failedLoginThreshold = failedLoginThreshold;
	}
	
	@Advanced
	public Long getFailedLoginWindow() {
		return failedLoginWindow;
	}
	public void setFailedLoginWindow(Long failedLoginWindow) {
		this.failedLoginWindow = failedLoginWindow;
	}
	
	@Advanced
	public Long getFailedLoginBlacklistDuration() {
		return failedLoginBlacklistDuration;
	}
	public void setFailedLoginBlacklistDuration(Long failedLoginBlacklistDuration) {
		this.failedLoginBlacklistDuration = failedLoginBlacklistDuration;
	}
	@Advanced
	public Long getMaxTotalScriptCacheSize() {
		return maxTotalScriptCacheSize;
	}
	public void setMaxTotalScriptCacheSize(Long maxTotalScriptCacheSize) {
		this.maxTotalScriptCacheSize = maxTotalScriptCacheSize;
	}
	@Advanced
	public Long getMaxScriptCacheSize() {
		return maxScriptCacheSize;
	}
	public void setMaxScriptCacheSize(Long maxScriptCacheSize) {
		this.maxScriptCacheSize = maxScriptCacheSize;
	}
	@Advanced
	public Long getScriptCacheTimeout() {
		return scriptCacheTimeout;
	}
	public void setScriptCacheTimeout(Long scriptCacheTimeout) {
		this.scriptCacheTimeout = scriptCacheTimeout;
	}
	@Advanced
	public String getSessionCacheId() {
		return sessionCacheId;
	}
	public void setSessionCacheId(String sessionCacheId) {
		this.sessionCacheId = sessionCacheId;
	}
	@Advanced
	@Comment(title = "This allows you to store the cookies for this web page on a different path than the actual web application which is the default path")
	public String getCookiePath() {
		return cookiePath;
	}
	public void setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
	}
	
	@Advanced
	@Comment(title = "This service allows you to add very custom request subscribers to a web application that have more contextual awareness than those at the host level")
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RequestSubscriber.handle")	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRequestSubscriber() {
		return requestSubscriber;
	}
	public void setRequestSubscriber(DefinedService requestSubscriber) {
		this.requestSubscriber = requestSubscriber;
	}
	
	@Advanced
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RateLimitSettingsProvider.settings")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRateLimiter() {
		return rateLimiter;
	}
	public void setRateLimiter(DefinedService rateLimiter) {
		this.rateLimiter = rateLimiter;
	}
	
	@Advanced
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public ListableSinkProviderArtifact getRateLimiterDatabase() {
		return rateLimiterDatabase;
	}
	public void setRateLimiterDatabase(ListableSinkProviderArtifact rateLimiterDatabase) {
		this.rateLimiterDatabase = rateLimiterDatabase;
	}
	
	@Advanced
	public boolean isAddCacheHeaders() {
		return addCacheHeaders;
	}
	public void setAddCacheHeaders(boolean addCacheHeaders) {
		this.addCacheHeaders = addCacheHeaders;
	}
		
	@Advanced
	@EnvironmentSpecific
	public String getJwtKeyAlias() {
		return jwtKeyAlias;
	}
	public void setJwtKeyAlias(String jwtKeyAlias) {
		this.jwtKeyAlias = jwtKeyAlias;
	}
	
	@Advanced
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getJwtKeyStore() {
		return jwtKeyStore;
	}
	public void setJwtKeyStore(KeyStoreArtifact jwtKeyStore) {
		this.jwtKeyStore = jwtKeyStore;
	}
	
	@Advanced
	public boolean isAllowJwtBearer() {
		return allowJwtBearer;
	}
	public void setAllowJwtBearer(boolean allowJwtBearer) {
		this.allowJwtBearer = allowJwtBearer;
	}
	
	@Advanced
	public boolean isAllowContentEncoding() {
		return allowContentEncoding;
	}
	public void setAllowContentEncoding(boolean allowContentEncoding) {
		this.allowContentEncoding = allowContentEncoding;
	}
	
	@Advanced
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@Comment(title = "You can configure an application if you want to reuse the sessions that already exist in that application", description = "If a session provider application is set, the other session parameters are ignored in this application")
	public WebApplication getSessionProviderApplication() {
		return sessionProviderApplication;
	}
	public void setSessionProviderApplication(WebApplication sessionProviderApplication) {
		this.sessionProviderApplication = sessionProviderApplication;
	}
	
	@Advanced
	@Comment(title = "All 404 that accept text/html will be served with the index page, assuming html5 mode in the frontend")
	public boolean isHtml5Mode() {
		return html5Mode;
	}
	public void setHtml5Mode(boolean html5Mode) {
		this.html5Mode = html5Mode;
	}
	
	@Advanced
	@Comment(title = "You can force the request language to win from the indirectly chosen browser-configured language")
	public boolean isForceRequestLanguage() {
		return forceRequestLanguage;
	}
	public void setForceRequestLanguage(boolean forceRequestLanguage) {
		this.forceRequestLanguage = forceRequestLanguage;
	}
	
	@Advanced
	public String getProxyPath() {
		return proxyPath;
	}
	public void setProxyPath(String proxyPath) {
		this.proxyPath = proxyPath;
	}
	public String getDefaultLanguage() {
		return defaultLanguage;
	}
	public void setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}
	
	@Advanced
	@Comment(title = "You can choose to actively ignore the language cookie")
	public boolean isIgnoreLanguageCookie() {
		return ignoreLanguageCookie;
	}
	public void setIgnoreLanguageCookie(boolean ignoreLanguageCookie) {
		this.ignoreLanguageCookie = ignoreLanguageCookie;
	}
	
}
