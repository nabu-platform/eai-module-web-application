package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.module.http.virtual.api.SourceImpl;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.glue.annotations.GlueParam;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.evaluator.annotations.MethodProviderClass;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.glue.impl.RequestMethods;
import be.nabu.libs.http.glue.impl.UserMethods;
import be.nabu.libs.nio.PipelineUtils;
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
	
	public HTTPResponse throttle(String action, String context) throws IOException {
		if (application.getRateLimiter() == null) {
			return null;
		}
		HTTPRequest request = (HTTPRequest) RequestMethods.entity();
		SourceImpl source = new SourceImpl(PipelineUtils.getPipeline().getSourceContext());
		return application.getRateLimiter().handle(application, request, source, UserMethods.token(), UserMethods.device(), action, context);
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
	
	public String language() throws IOException {
		return WebApplicationUtils.getLanguage(application, (HTTPRequest) RequestMethods.entity());
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
