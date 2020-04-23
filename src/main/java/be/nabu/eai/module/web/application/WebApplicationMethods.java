package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.LanguageProvider;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.glue.annotations.GlueParam;
import be.nabu.glue.api.Script;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.glue.utils.ScriptUtils;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.evaluator.annotations.MethodProviderClass;
import be.nabu.libs.http.api.HTTPEntity;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.LinkableHTTPResponse;
import be.nabu.libs.http.glue.impl.RequestMethods;
import be.nabu.libs.http.glue.impl.UserMethods;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;

@MethodProviderClass(namespace = "application")
public class WebApplicationMethods {
	
	private WebApplication application;

	public WebApplicationMethods(WebApplication application) {
		this.application = application;
	}
	
	public Iterable<String> fragmentResources(String path) throws IOException {
		return providedFolders(application, path, new ArrayList<String>());
	}
	
	/**
	 * This method will first list the matching paths in the child fragments, and only than in the parent fragmentprovider
	 * This is because if the parent includes the children, he most likely has a dependency on them for some reason, which is usually the reason that you want the paths in the first place
	 */
	private List<String> providedFolders(WebFragmentProvider provider, String path, List<String> blacklist) throws IOException {
		List<String> folders = new ArrayList<String>();
		if (provider.getWebFragments() != null) {
			for (WebFragment fragment : provider.getWebFragments()) {
				if (fragment instanceof WebFragmentProvider) {
					if (blacklist.contains(fragment.getId())) {
						continue;
					}
					else {
						blacklist.add(fragment.getId());
					}
					folders.addAll(providedFolders((WebFragmentProvider) fragment, path, blacklist));
				}
			}
		}
		if (provider instanceof Artifact) {
			Entry entry = application.getRepository().getEntry(((Artifact) provider).getId());
			if (entry instanceof ResourceEntry) {
				ResourceContainer<?> container = ((ResourceEntry) entry).getContainer();
				boolean exists = container == null ? false : ResourceUtils.resolve(container, path) != null;
				if (exists) {
					folders.add("repository:" + ((Artifact) provider).getId() + ":/" + path);
				}
			}
		}
		return folders;
	}
	
	public ComplexContent configuration(@GlueParam(name = "type") String type, @GlueParam(name = "path") String path) throws IOException {
		return application.getConfigurationFor(path == null ? "/" : path, (ComplexType) DefinedTypeResolverFactory.getInstance().getResolver().resolve(type));
	}
	
	// for throttling, you can currently only use the script level defined rate limit action with a runtime context
	// the reason for this is we want to be able to statically extract the rate limit actions being used, by defining it in an annotation it can be statically extracted
	// in the future we might allow a second parameter here to specify a custom action if the need calls for it
	public HTTPResponse throttle(@GlueParam(name = "context") String context) throws IOException, ParseException {
		if (application.getRateLimiter() == null) {
			return null;
		}
		HTTPRequest request = (HTTPRequest) RequestMethods.entity();
		Script script = ScriptRuntime.getRuntime().getScript();
		String rateLimitAction = script.getRoot().getContext().getAnnotations().get("limit");
		if (rateLimitAction == null) {
			rateLimitAction = script.getRoot().getContext().getAnnotations().get("operationId");
			if (rateLimitAction == null) {
				rateLimitAction = ScriptUtils.getFullName(script);
			}
		}
		return WebApplicationUtils.checkRateLimits(application, UserMethods.token(), UserMethods.device(), rateLimitAction, context, request);
	}
	
	public RoleHandler roleHandler() throws IOException {
		return application.getRoleHandler();
	}
	
	public PermissionHandler permissionHandler() throws IOException {
		return application.getPermissionHandler();
	}
	
	public Authenticator authenticator() {
		return application.getAuthenticator();
	}
	
	public String applicationLanguage() throws IOException {
		HTTPEntity entity = RequestMethods.entity();
		HTTPRequest request = null;
		if (entity instanceof HTTPRequest) {
			request = (HTTPRequest) entity;
		}
		else if (entity instanceof LinkableHTTPResponse) {
			request = ((LinkableHTTPResponse) entity).getRequest();
		}
		if (request != null) {
			return WebApplicationUtils.getApplicationLanguage(application, request);
		}
		return null;
	}
	
	public String language() throws IOException {
		HTTPEntity entity = RequestMethods.entity();
		HTTPRequest request = null;
		if (entity instanceof HTTPRequest) {
			request = (HTTPRequest) entity;
		}
		else if (entity instanceof LinkableHTTPResponse) {
			request = ((LinkableHTTPResponse) entity).getRequest();
		}
		if (request != null) {
			return WebApplicationUtils.getLanguage(application, request);
		}
		return null;
	}

	public List<String> languages() throws IOException {
		LanguageProvider languageProvider = application.getLanguageProvider();
		if (languageProvider != null) {
			return languageProvider.getSupportedLanguages();
		}
		return null;
	}
	
	public void stop(String id, String path) {
		Artifact resolve = application.getRepository().resolve(id);
		if (resolve instanceof WebFragment) {
			((WebFragment) resolve).stop(application, path);
		}
	}
	
	// you can reject a request with a specific code that (could) be whitelisted by the application to show the user
	public static void reject(@GlueParam(name = "code") String code, @GlueParam(name = "message") String message) throws ServiceException {
		throw new ServiceException(code, message);
	}

}
