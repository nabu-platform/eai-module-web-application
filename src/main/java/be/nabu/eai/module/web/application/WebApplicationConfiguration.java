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

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.Hidden;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.api.CacheProviderArtifact;
import be.nabu.eai.repository.api.VirusScanner;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.http.core.SameSite;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.annotation.Field;

@XmlRootElement(name = "webApplication")
@XmlType(propOrder = { "virtualHost", "realm", "path", "cookiePath", "defaultCookieSitePolicy", "charset", "allowBasicAuthentication", "failedLoginThreshold", "failedLoginWindow",
		"failedLoginBlacklistDuration", "passwordAuthenticationService", "secretAuthenticationService", "secretGeneratorService", "bearerAuthenticator", "arbitraryAuthenticator", "temporaryAuthenticator",  
		"temporaryAuthenticationGenerator", "temporaryAuthenticationRevoker", "typedAuthenticationService", "permissionService", "potentialPermissionService", "roleService", "tokenValidatorService", 
		"deviceValidatorService", "translationService", "supportedLanguagesService", "languageProviderService", "requestLanguageProviderService", 
		"defaultLanguage", "rateLimitSettings", "rateLimitChecker", "rateLimitLogger", "rateLimiter", "corsChecker", "requestSubscriber", "whitelistedCodes", "sessionCacheProvider", "sessionCacheId", 
		"maxTotalSessionSize", "maxSessionSize", "sessionTimeout", "sessionProviderApplication", "scriptCacheProvider", "maxTotalScriptCacheSize", 
		"maxScriptCacheSize", "scriptCacheTimeout", "addCacheHeaders", "jwtKeyStore", "jwtKeyAlias", "allowJwtBearer", "allowContentEncoding", "services", 
		"webFragments", "html5Mode", "optimizedLoad", "forceRequestLanguage", "proxyPath", "ignoreLanguageCookie", "testRole", "contextSwitchingRole", "virusScanner", "stateless", "frameOption" })
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
	// which roles can test toggling features
	private List<String> testRole;
	private FrameOption frameOption;
	
	// which roles can switch between contexts
	// if nothing is defined, we don't do a role check and only a permission check
	private List<String> contextSwitchingRole;
	
	// we assume the proxy strips the path, but to build correct links for the outside world, we need to know this
	private String proxyPath;
	
	private DefinedService passwordAuthenticationService, secretAuthenticationService, secretGeneratorService;
	private DefinedService permissionService, potentialPermissionService;
	private DefinedService roleService;
	private DefinedService tokenValidatorService;
	private DefinedService translationService, supportedLanguagesService;
	private DefinedService languageProviderService, requestLanguageProviderService;
	private DefinedService deviceValidatorService;
	private DefinedService requestSubscriber;
	private DefinedService bearerAuthenticator, arbitraryAuthenticator;
	private DefinedService temporaryAuthenticator, temporaryAuthenticationGenerator, temporaryAuthenticationRevoker;
	private DefinedService typedAuthenticationService;
	private DefinedService corsChecker;
	private Boolean allowBasicAuthentication;
	private List<WebFragment> webFragments;
	// services to expose
	private List<DefinedService> services;
	// you can reuse the sessions from another application
	private WebApplication sessionProviderApplication;
	
	private SameSite defaultCookieSitePolicy;
	
	/**
	 * You can define jwt keystore and alias in the web application itself
	 * JWT tokens can be used for a whole host of reasons, and having to configure this in every separate module is annoying
	 * You can still provide overrides in the modules if you want, but there should be a default for each web application
	 * The tokens you generate here are mostly meant to be consumed by the same web application, so symmetric encryption is vastly preferred for speed & size considerations
	 */
	private String jwtKeyAlias;
	private KeyStoreArtifact jwtKeyStore;
	
	// the optimized load option will store the last compiled version of javascript & css AND minify it upon commit
	// in the target environment (assuming development mode is turned off), this highly optimized compiled version is used instead of recalculating everything
	private boolean allowJwtBearer, allowContentEncoding = true, html5Mode, optimizedLoad;
	private boolean forceRequestLanguage;
	
	// in a stateless application, we don't have sessions at all
	private boolean stateless;
	
	private DefinedService rateLimitSettings, rateLimitChecker, rateLimitLogger, rateLimiter;
	
	private VirusScanner virusScanner;
	
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

	@Field(group = "language", comment = "This service is responsible for translating content in the web application. Use the syntax %{The sentence} to encapsulate content that has to be translated")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.Translator.translate")
	public DefinedService getTranslationService() {
		return translationService;
	}
	public void setTranslationService(DefinedService translationService) {
		this.translationService = translationService;
	}
	
	@Field(group = "language", comment = "List all of the available languages")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.LanguageProvider.getSupportedLanguages")
	public DefinedService getSupportedLanguagesService() {
		return supportedLanguagesService;
	}
	public void setSupportedLanguagesService(DefinedService supportedLanguagesService) {
		this.supportedLanguagesService = supportedLanguagesService;
	}
	
	@Field(group = "language", comment = "This service is responsible for indicating which language a user wants based on e.g. profile settings")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.repository.api.UserLanguageProvider.getLanguage")	
	public DefinedService getLanguageProviderService() {
		return languageProviderService;
	}
	public void setLanguageProviderService(DefinedService languageProviderService) {
		this.languageProviderService = languageProviderService;
	}
	
	@Field(group = "language", comment = "This service is responsible for indicating which language a user wants based on the request.")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RequestLanguageProvider.getLanguage")	
	public DefinedService getRequestLanguageProviderService() {
		return requestLanguageProviderService;
	}
	public void setRequestLanguageProviderService(DefinedService requestLanguageProviderService) {
		this.requestLanguageProviderService = requestLanguageProviderService;
	}
	
	@Field(group = "security", comment = "This service is responsible for authenticating a username with a given password. If a secret is returned in the response of this service, the secret authenticator can be used to remember users automatically.",
		hide = "typedAuthenticationService != null")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.PasswordAuthenticator.authenticate")
	public DefinedService getPasswordAuthenticationService() {
		return passwordAuthenticationService;
	}
	public void setPasswordAuthenticationService(DefinedService passwordAuthenticationService) {
		this.passwordAuthenticationService = passwordAuthenticationService;
	}

	@Field(group = "security", comment = "This service is responsible for creating a valid token from a bearer authentication.")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.BearerAuthenticator.authenticate")
	public DefinedService getBearerAuthenticator() {
		return bearerAuthenticator;
	}
	public void setBearerAuthenticator(DefinedService bearerAuthenticator) {
		this.bearerAuthenticator = bearerAuthenticator;
	}
	
	@Field(group = "security", comment = "This service is responsible for creating a valid token from the generic http request.")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.ArbitraryAuthenticator.authenticate")
	public DefinedService getArbitraryAuthenticator() {
		return arbitraryAuthenticator;
	}
	public void setArbitraryAuthenticator(DefinedService arbitraryAuthenticator) {
		this.arbitraryAuthenticator = arbitraryAuthenticator;
	}
	
	@Field(group = "security", comment = "This service is responsible for remembering a previously logged in user based on a shared secret.",
		hide = "typedAuthenticationService != null")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.SecretAuthenticator.authenticate")
	public DefinedService getSecretAuthenticationService() {
		return secretAuthenticationService;
	}
	public void setSecretAuthenticationService(DefinedService secretAuthenticationService) {
		this.secretAuthenticationService = secretAuthenticationService;
	}
	
	@Field(group = "security", comment = "This service is responsible for generating a secret for a token so we can be remembered later on",
		hide = "stateless == true")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.SecretGenerator.generate")
	public DefinedService getSecretGeneratorService() {
		return secretGeneratorService;
	}
	public void setSecretGeneratorService(DefinedService secretGeneratorService) {
		this.secretGeneratorService = secretGeneratorService;
	}
	
	@Field(group = "security", comment = "This service is reponsible for temporarily authenticating someone to perform a certain action")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.TemporaryAuthenticator.authenticate")
	public DefinedService getTemporaryAuthenticator() {
		return temporaryAuthenticator;
	}
	public void setTemporaryAuthenticator(DefinedService temporaryAuthenticator) {
		this.temporaryAuthenticator = temporaryAuthenticator;
	}
	
	@Field(group = "security", comment = "Typed authenticator is a catch all for anything that combines an identifier with a secret (password, secret, temporary, oauth2,...)")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.authentication.api.TypedAuthenticator.authenticate")
	public DefinedService getTypedAuthenticationService() {
		return typedAuthenticationService;
	}
	public void setTypedAuthenticationService(DefinedService typedAuthenticationService) {
		this.typedAuthenticationService = typedAuthenticationService;
	}
	
	@Field(group = "security", comment = "This service is responsible for generating temporary authentication tokens")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.TemporaryAuthenticationGenerator.generate")
	public DefinedService getTemporaryAuthenticationGenerator() {
		return temporaryAuthenticationGenerator;
	}
	public void setTemporaryAuthenticationGenerator(DefinedService temporaryAuthenticationGenerator) {
		this.temporaryAuthenticationGenerator = temporaryAuthenticationGenerator;
	}
	
	@Field(group = "security", comment = "This service is responsible for revoking temporary authentication tokens")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.TemporaryAuthenticationRevoker.revoke")	
	public DefinedService getTemporaryAuthenticationRevoker() {
		return temporaryAuthenticationRevoker;
	}
	public void setTemporaryAuthenticationRevoker(DefinedService temporaryAuthenticationRevoker) {
		this.temporaryAuthenticationRevoker = temporaryAuthenticationRevoker;
	}
	
	@Field(group = "security", comment = "This service is responsible for checking if a user has a specific permission. A permission is the combination of an action (what are you trying to do) in an optional context (e.g. a CMS node).")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.PermissionHandler.hasPermission")
	public DefinedService getPermissionService() {
		return permissionService;
	}
	public void setPermissionService(DefinedService permissionService) {
		this.permissionService = permissionService;
	}

	@Field(group = "security", comment = "This service is responsible for checking if a user has a specific role")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.RoleHandler.hasRole")
	public DefinedService getRoleService() {
		return roleService;
	}
	public void setRoleService(DefinedService roleService) {
		this.roleService = roleService;
	}

	@Field(group = "security", comment = "This service is responsible for checking if a user potentially has a permission regardless of the context. Once a context is known, the permission handler might still not grant the permission though.")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.PotentialPermissionHandler.hasPotentialPermission")
	public DefinedService getPotentialPermissionService() {
		return potentialPermissionService;
	}
	public void setPotentialPermissionService(DefinedService potentialPermissionService) {
		this.potentialPermissionService = potentialPermissionService;
	}
	
	@Field(group = "security", comment = "This service is responsible for checking if a previously granted token is still valid.", hide = "stateless == true")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.TokenValidator.isValid")
	public DefinedService getTokenValidatorService() {
		return tokenValidatorService;
	}
	public void setTokenValidatorService(DefinedService tokenValidatorService) {
		this.tokenValidatorService = tokenValidatorService;
	}

	@Field(group = "security", comment = "This service is responsible for checking if a certain device is allowed for a given user.", hide = "stateless == true")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.DeviceValidator.isAllowed")	
	public DefinedService getDeviceValidatorService() {
		return deviceValidatorService;
	}
	public void setDeviceValidatorService(DefinedService deviceValidatorService) {
		this.deviceValidatorService = deviceValidatorService;
	}
	
	@Field(group = "security", comment = "By default basic authentication is not enabled. Especially when building API's however it can be interesting to turn this on.")
	public Boolean getAllowBasicAuthentication() {
		return allowBasicAuthentication;
	}
	public void setAllowBasicAuthentication(Boolean allowBasicAuthentication) {
		this.allowBasicAuthentication = allowBasicAuthentication;
	}

	@Field(comment = "The realm this web application operates in. This can be important for authentication, especially in shared environments.", group = "security", environmentSpecific = true)
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

	@Field(group = "caching", comment = "You can configure a session cache provider for this web application. All the session information will be stored there.", hide = "stateless == true")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public CacheProviderArtifact getSessionCacheProvider() {
		return sessionCacheProvider;
	}
	public void setSessionCacheProvider(CacheProviderArtifact sessionCacheProvider) {
		this.sessionCacheProvider = sessionCacheProvider;
	}
	
	@Field(group = "caching", comment = "The script cache provider is used to temporarily cache page results, _always_ use a serializing cache for this and _never_ cache a page with user-specific content on it! You can set a '@cache <timeout>' annotation at the top of a script to enable caching. The timeout is optional and can be set to 0 for indefinite cached values.",
		hide = "optimizedLoad == true")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public CacheProviderArtifact getScriptCacheProvider() {
		return scriptCacheProvider;
	}
	public void setScriptCacheProvider(CacheProviderArtifact scriptCacheProvider) {
		this.scriptCacheProvider = scriptCacheProvider;
	}
	
	@Field(group = "caching", comment = "The max total size of all the active sessions.", hide = "stateless == true")
	public Long getMaxTotalSessionSize() {
		return maxTotalSessionSize;
	}
	public void setMaxTotalSessionSize(Long maxTotalSessionSize) {
		this.maxTotalSessionSize = maxTotalSessionSize;
	}

	@Field(group = "caching", comment = "The max size of a single session.", hide = "stateless == true")
	public Long getMaxSessionSize() {
		return maxSessionSize;
	}
	public void setMaxSessionSize(Long maxSessionSize) {
		this.maxSessionSize = maxSessionSize;
	}

	@Field(group = "caching", comment = "How long it takes a session to time out. The timeout is based on the last time the session was accessed so it is a moving window.", hide = "stateless == true")
	public Long getSessionTimeout() {
		return sessionTimeout;
	}
	public void setSessionTimeout(Long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}
	
	@Field(comment = "Add web fragments to be exposed, for example REST services, WSDL services, web components...")
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
	
	@Hidden
	@Advanced
	public Long getFailedLoginThreshold() {
		return failedLoginThreshold;
	}
	public void setFailedLoginThreshold(Long failedLoginThreshold) {
		this.failedLoginThreshold = failedLoginThreshold;
	}
	
	@Hidden
	@Advanced
	public Long getFailedLoginWindow() {
		return failedLoginWindow;
	}
	public void setFailedLoginWindow(Long failedLoginWindow) {
		this.failedLoginWindow = failedLoginWindow;
	}
	
	@Hidden
	@Advanced
	public Long getFailedLoginBlacklistDuration() {
		return failedLoginBlacklistDuration;
	}
	public void setFailedLoginBlacklistDuration(Long failedLoginBlacklistDuration) {
		this.failedLoginBlacklistDuration = failedLoginBlacklistDuration;
	}
	
	@Field(group = "caching", comment = "The max total size of all the cached scripts.", hide = "optimizedLoad == true")
	public Long getMaxTotalScriptCacheSize() {
		return maxTotalScriptCacheSize;
	}
	public void setMaxTotalScriptCacheSize(Long maxTotalScriptCacheSize) {
		this.maxTotalScriptCacheSize = maxTotalScriptCacheSize;
	}
	
	@Field(group = "caching", comment = "The max size of a single cached script.", hide = "optimizedLoad == true")
	public Long getMaxScriptCacheSize() {
		return maxScriptCacheSize;
	}
	public void setMaxScriptCacheSize(Long maxScriptCacheSize) {
		this.maxScriptCacheSize = maxScriptCacheSize;
	}
	
	@Field(group = "caching", comment = "How long it takes for a script cache to time (unless specified otherwise on the script itself). The timeout is based on when the cache was created.", hide = "optimizedLoad == true")
	public Long getScriptCacheTimeout() {
		return scriptCacheTimeout;
	}
	public void setScriptCacheTimeout(Long scriptCacheTimeout) {
		this.scriptCacheTimeout = scriptCacheTimeout;
	}
	
	@Field(group = "caching", comment = "You can configure a custom session cache id. This can be used to create complex shared session caches.", hide = "stateless == true")
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
	
	@Field(group = "rateLimiting", comment = "Set a service that provides the rate limit settings for a specific request.")
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RateLimitProvider.settings")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRateLimitSettings() {
		return rateLimitSettings;
	}
	public void setRateLimitSettings(DefinedService rateLimitSettingsProvider) {
		this.rateLimitSettings = rateLimitSettingsProvider;
	}
	
	@Field(group = "rateLimiting", comment = "Allow rate limiting of a request")
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RateLimitProvider.rateLimit")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRateLimiter() {
		return rateLimiter;
	}
	public void setRateLimiter(DefinedService rateLimiter) {
		this.rateLimiter = rateLimiter;
	}
	
	@Field(group = "crossOrigin", comment = "Choose the frame policy for this application, default is DENY")
	public FrameOption getFrameOption() {
		return frameOption;
	}
	public void setFrameOption(FrameOption frameOption) {
		this.frameOption = frameOption;
	}
	
	@Field(group = "crossOrigin", comment = "Set a service that checks the CORS policies")
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.CORSHandler.check")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getCorsChecker() {
		return corsChecker;
	}
	public void setCorsChecker(DefinedService corsChecker) {
		this.corsChecker = corsChecker;
	}
	
	@Field(group = "rateLimiting", comment = "Set a service that will check the rate limits for a specific request")
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RateLimitProvider.check")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRateLimitChecker() {
		return rateLimitChecker;
	}
	public void setRateLimitChecker(DefinedService rateLimitChecker) {
		this.rateLimitChecker = rateLimitChecker;
	}
	
	@Field(group = "rateLimiting", comment = "Set a service that will log a rate limit hit")
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RateLimitProvider.log")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getRateLimitLogger() {
		return rateLimitLogger;
	}
	public void setRateLimitLogger(DefinedService rateLimitLogger) {
		this.rateLimitLogger = rateLimitLogger;
	}
	
	@Field(group = "caching", comment = "When this enabled, explicit no-cache directives will be injected in responses in the absence of other cache headers. This enforces correct caching behavior in browsers.")
	public boolean isAddCacheHeaders() {
		return addCacheHeaders;
	}
	public void setAddCacheHeaders(boolean addCacheHeaders) {
		this.addCacheHeaders = addCacheHeaders;
	}
		
	@Field(group = "security", comment = "Set the default key alias to use when generating jwt tokens for this web application. It should point to a key in the accompanying keystore property. Generally speaking, a symmetric key is the best option here.")
	@EnvironmentSpecific
	public String getJwtKeyAlias() {
		return jwtKeyAlias;
	}
	public void setJwtKeyAlias(String jwtKeyAlias) {
		this.jwtKeyAlias = jwtKeyAlias;
	}
	
	@Field(group = "security", comment = "Set the default keystore to use when generating jwt tokens for this web application.")
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getJwtKeyStore() {
		return jwtKeyStore;
	}
	public void setJwtKeyStore(KeyStoreArtifact jwtKeyStore) {
		this.jwtKeyStore = jwtKeyStore;
	}
	
	// there is a generic bearer handler principle, allowing for much more freedom as to how you want to deal with this
	@Deprecated
	@Hidden
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
	
	@Field(comment = "By enabling HTML5 mode, you can use proper paths in an SPA frontend rather than having to resort to hashtag-based routing. In practice all 404 that accept text/html will be served with the index page rather than a 404.")
	public boolean isHtml5Mode() {
		return html5Mode;
	}
	public void setHtml5Mode(boolean html5Mode) {
		this.html5Mode = html5Mode;
	}
	
	@Field(group = "language", comment = "You can force the request language to win from the indirectly chosen browser-configured language.")
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
	
	@Field(group = "language", comment = "The default language for this application.")
	public String getDefaultLanguage() {
		return defaultLanguage;
	}
	public void setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}
	
	@Field(group = "language", comment = "You can choose to actively ignore the language cookie.")
	public boolean isIgnoreLanguageCookie() {
		return ignoreLanguageCookie;
	}
	public void setIgnoreLanguageCookie(boolean ignoreLanguageCookie) {
		this.ignoreLanguageCookie = ignoreLanguageCookie;
	}
	
	@Field(group = "security", comment = "The people who have this role can perform additional actions as testers, for example toggle features.")
	public List<String> getTestRole() {
		return testRole;
	}
	public void setTestRole(List<String> testRole) {
		this.testRole = testRole;
	}
	
	@Field(group = "security", comment = "The people who have this role can choose a different execution context in the frontend. If you have a permission handler, that will always be checked as well.")
	public List<String> getContextSwitchingRole() {
		return contextSwitchingRole;
	}
	public void setContextSwitchingRole(List<String> contextSwitchingRole) {
		this.contextSwitchingRole = contextSwitchingRole;
	}
	
	@Comment(title = "Services you want to expose through this web application")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<DefinedService> getServices() {
		return services;
	}
	public void setServices(List<DefinedService> services) {
		this.services = services;
	}
	

	@Field(group = "security", comment = "Configure a virus scanner for this web application. Once configured, all binary uploads will automatically be scanned for viruses.")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public VirusScanner getVirusScanner() {
		return virusScanner;
	}
	public void setVirusScanner(VirusScanner virusScanner) {
		this.virusScanner = virusScanner;
	}
	
	@Field(comment = "By enabling optimization, you compile the application in development. The advantage is instant production availability of compilable resources. The downside is there must be no difference between the environments")
	public boolean isOptimizedLoad() {
		return optimizedLoad;
	}
	public void setOptimizedLoad(boolean optimizedLoad) {
		this.optimizedLoad = optimizedLoad;
	}
	
	@Field(comment = "By enabling statelessness, sessions are disabled on the web application.")
	public boolean isStateless() {
		return stateless;
	}
	public void setStateless(boolean stateless) {
		this.stateless = stateless;
	}
	
	@Advanced
	@Comment(title = "The default same site policy for cookies set in this application")
	public SameSite getDefaultCookieSitePolicy() {
		return defaultCookieSitePolicy;
	}
	public void setDefaultCookieSitePolicy(SameSite defaultCookieSitePolicy) {
		this.defaultCookieSitePolicy = defaultCookieSitePolicy;
	}

}
