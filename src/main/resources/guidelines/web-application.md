# Artifact: webApplication

Fragments:
- `metadata.xml`: repository metadata around the artifact
- `web-application.xml`: the web application configuration

## Fragment: web-application.xml

Schema:
```ts
type WebApplication = {
	virtualHost?: ArtifactRef;
	realm?: string;
	path?: string;
	cookiePath?: string;
	defaultCookieSitePolicy?: "LAX" | "STRICT" | "NONE";
	charset?: string;
	allowBasicAuthentication?: boolean;
	bearerAuthenticator?: ServiceRef;
	temporaryAuthenticator?: ServiceRef;
	temporaryAuthenticationGenerator?: ServiceRef;
	temporaryAuthenticationRevoker?: ServiceRef;
	typedAuthenticationService?: ServiceRef;
	permissionService?: ServiceRef;
	potentialPermissionService?: ServiceRef;
	roleService?: ServiceRef;
	translationService?: ServiceRef;
	supportedLanguagesService?: ServiceRef;
	languageProviderService?: ServiceRef;
	defaultLanguage?: string;
	rateLimitSettings?: ServiceRef;
	rateLimitChecker?: ServiceRef;
	rateLimitLogger?: ServiceRef;
	rateLimiter?: ServiceRef;
	corsChecker?: ServiceRef;
	whitelistedCodes?: string;
	addCacheHeaders?: boolean;
	jwtKeyStore?: ArtifactRef;
	jwtKeyAlias?: string;
	allowContentEncoding?: boolean;
	webFragments?: unknown[];
	html5Mode?: boolean;
	forceRequestLanguage?: boolean;
	proxyPath?: string;
	ignoreLanguageCookie?: boolean;
	virusScanner?: ArtifactRef;
	frameOption?: string;
};
```

Fields:
- `virtualHost`: virtual host artifact the application is attached to.
- `realm`: security realm name for the application.
- `path`: root path this application listens on.
- `cookiePath`: optional cookie path override.
- `defaultCookieSitePolicy`: default SameSite policy for cookies.
- `charset`: default response charset.
- `allowBasicAuthentication`: enables HTTP Basic auth handling.
- `bearerAuthenticator`: service that turns bearer auth into a valid token. Required spec: `be.nabu.eai.module.web.application.api.BearerAuthenticator.authenticate`.
- `temporaryAuthenticator`: service for temporary authentication flows. Required spec: `be.nabu.eai.module.web.application.api.TemporaryAuthenticator.authenticate`.
- `temporaryAuthenticationGenerator`: service that creates temporary auth tokens. Required spec: `be.nabu.eai.module.web.application.api.TemporaryAuthenticationGenerator.generate`.
- `temporaryAuthenticationRevoker`: service that revokes temporary auth tokens. Required spec: `be.nabu.eai.module.web.application.api.TemporaryAuthenticationRevoker.revoke`.
- `typedAuthenticationService`: generic typed authenticator covering multiple auth styles. Required spec: `be.nabu.eai.authentication.api.TypedAuthenticator.authenticate`.
- `permissionService`: checks whether a user has a permission in context. Required spec: `be.nabu.libs.authentication.api.PermissionHandler.hasPermission`.
- `potentialPermissionService`: checks possible permission without full context. Required spec: `be.nabu.libs.authentication.api.PotentialPermissionHandler.hasPotentialPermission`.
- `roleService`: checks whether a user has a role. Required spec: `be.nabu.libs.authentication.api.RoleHandler.hasRole`.
- `translationService`: translates application content. Required spec: `be.nabu.eai.repository.api.Translator.translate`.
- `supportedLanguagesService`: returns supported languages. Required spec: `be.nabu.eai.repository.api.LanguageProvider.getSupportedLanguages`.
- `languageProviderService`: chooses language from user/profile context. Required spec: `be.nabu.eai.repository.api.UserLanguageProvider.getLanguage`.
- `defaultLanguage`: fallback language.
- `rateLimitSettings`: provides rate limit settings per request. Required spec: `be.nabu.eai.module.web.application.api.RateLimitProvider.settings`.
- `rateLimitChecker`: checks current rate limit state. Required spec: `be.nabu.eai.module.web.application.api.RateLimitProvider.check`.
- `rateLimitLogger`: logs rate limit hits. Required spec: `be.nabu.eai.module.web.application.api.RateLimitProvider.log`.
- `rateLimiter`: applies rate limiting decisions. Required spec: `be.nabu.eai.module.web.application.api.RateLimitProvider.rateLimit`.
- `corsChecker`: service that evaluates CORS policy. Required spec: `be.nabu.eai.module.web.application.api.CORSHandler.check`.
- `whitelistedCodes`: comma-separated backend error codes exposed to the frontend.
- `addCacheHeaders`: injects no-cache headers when needed.
- `jwtKeyStore`: default keystore for JWT generation.
- `jwtKeyAlias`: default key alias in the JWT keystore.
- `allowContentEncoding`: allows compressed/encoded content handling.
- `webFragments`: exposed web fragments such as REST, WSDL, components, etc. Search for artifacts with `webFragment=true`
- `html5Mode`: serves SPA index content for matching HTML 404 routes.
- `forceRequestLanguage`: lets request-derived language override browser choice.
- `proxyPath`: external proxy path used for link generation.
- `ignoreLanguageCookie`: ignores stored language cookie.
- `virusScanner`: scanner used for uploaded binary content.
- `frameOption`: frame/embed policy for the application.

