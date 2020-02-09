package nabu.web.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import nabu.web.application.types.Cookie;
import nabu.web.application.types.PropertyImpl;
import nabu.web.application.types.WebApplicationInformation;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationUtils;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.application.WebFragmentProvider;
import be.nabu.eai.module.web.application.api.PermissionWithRole;
import be.nabu.eai.module.web.application.api.TemporaryAuthentication;
import be.nabu.eai.module.web.application.api.TemporaryAuthenticationGenerator;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.LanguageProvider;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.events.ResourceEvent;
import be.nabu.eai.repository.events.ResourceEvent.ResourceState;
import be.nabu.glue.impl.ImperativeSubstitutor;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.ResourceFilter;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.Header;

@WebService
public class Services {
	private ExecutionContext executionContext;

	@WebResult(name = "cookies")
	public List<Cookie> cookies(@WebParam(name = "headers") List<Header> headers) {
		Map<String, List<String>> cookies = HTTPUtils.getCookies(headers.toArray(new Header[0]));
		List<Cookie> list = new ArrayList<Cookie>();
		for (Map.Entry<String, List<String>> entry : cookies.entrySet()) {
			Cookie cookie = new Cookie();
			cookie.setName(entry.getKey());
			cookie.setValues(entry.getValue());
			list.add(cookie);
		}
		return list;
	}
	
	@WebResult(name = "permissions")
	public List<Permission> permissions(@NotNull @WebParam(name = "webApplicationId") String id, @WebParam(name = "role") String role) {
		List<Permission> permissions = new ArrayList<Permission>();
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				permissions(resolved, permissions);
			}
		}
		if (role != null) {
			Iterator<Permission> iterator = permissions.iterator();
			while (iterator.hasNext()) {
				Permission next = iterator.next();
				boolean keep = false;
				// we are listing permissions in this service, a permission consists of at least an action
				// if there is no action, we don't report it
				if (next.getAction() != null && next instanceof PermissionWithRole) {
					keep = (((PermissionWithRole) next).getRoles() != null && ((PermissionWithRole) next).getRoles().contains(role));
				}
				if (!keep) {
					iterator.remove();
				}
			}
		}
		return permissions;
	}
	
	@WebResult(name = "roles")
	public List<String> roles(@NotNull @WebParam(name = "webApplicationId") String id) {
		List<Permission> permissions = new ArrayList<Permission>();
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				permissions(resolved, permissions);
			}
		}
		List<String> roles = new ArrayList<String>();
		for (Permission permission : permissions) {
			if (permission instanceof PermissionWithRole) {
				List<String> possible = ((PermissionWithRole) permission).getRoles();
				if (possible != null) {
					for (String role : possible) {
						if (!roles.contains(role)) {
							roles.add(role);
						}
					}
				}
			}
		}
		return roles;
	}

	@WebResult(name = "translation")
	public String translate(@NotNull @WebParam(name = "webApplicationId") String id, @WebParam(name = "category") String category, @NotNull @WebParam(name = "key") String key, @WebParam(name = "language") String language) throws IOException {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Invalid web application: " + id);
		}
		return resolved.getTranslator() == null ? key : resolved.getTranslator().translate(category, key, language);
	}
	
	public boolean hasRole(@NotNull @WebParam(name = "webApplicationId") String id, @WebParam(name = "token") Token token, @WebParam(name = "role") String role) throws IOException {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Invalid web application: " + id);
		}
		// if there is no role handler, it is always ok
		return resolved.getRoleHandler() == null ? true : resolved.getRoleHandler().hasRole(token, role);
	}
	
	public boolean hasPermission(@NotNull @WebParam(name = "webApplicationId") String id, @WebParam(name = "token") Token token, @WebParam(name = "context") String context, @WebParam(name = "action") String action) throws IOException {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Invalid web application: " + id);
		}
		// if there is no role handler, it is always ok
		return resolved.getPermissionHandler() == null ? true : resolved.getPermissionHandler().hasPermission(token, context, action);
	}
	
	private void permissions(WebApplication application, List<Permission> permissions) {
		String path = application.getConfig().getPath();
		if (path == null) {
			path = "/";
		}
		for (WebFragment fragment : application.getWebFragments()) {
			if (fragment != null) {
				List<Permission> fragmentPermissions = fragment.getPermissions(application, path);
				if (fragmentPermissions != null) {
					permissions.addAll(fragmentPermissions);
				}
			}
		}
		// don't recurse, the web fragment providers should do that themselves
	}
	
	@WebResult(name = "translationKeys")
	public List<KeyValuePair> translationKeys(@NotNull @WebParam(name = "webApplicationId") String id) throws IOException {
		List<KeyValuePair> properties = new ArrayList<KeyValuePair>();
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				List<String> uniques = new ArrayList<String>();
				translationKeys(resolved, properties, uniques);
//				for (Script script : resolved.getListener().getRepository()) {
//					String fullName = ScriptUtils.getFullName(script);
//					try {
//						InputStream input = script.getSource();
//						try {
//							byte[] bytes = IOUtils.toBytes(IOUtils.wrap(input));
//							String source = new String(bytes, script.getCharset());
//							for (String key : ImperativeSubstitutor.getValues("%", source)) {
//								properties.add(new PropertyImpl("page:" + fullName, key));
//							}
//						}
//						finally {
//							input.close();
//						}
//					}
//					catch (IOException e) {
//						logger.error("Could not load source code for script: " + fullName);
//					}
//				}
			}
			else {
				throw new IllegalArgumentException("Can not find web artifact: " + id);
			}
		}
		return properties;
	}
	
	private void translationKeys(Artifact provider, List<KeyValuePair> keys, List<String> uniques) throws IOException {
		Entry entry = EAIResourceRepository.getInstance().getEntry(provider.getId());
		if (entry instanceof ResourceEntry) {
			ResourceContainer<?> container = ((ResourceEntry) entry).getContainer();
			translationKeys(container, keys, false, uniques);
			translationKeys((ResourceContainer<?>) container.getChild(EAIResourceRepository.PUBLIC), keys, true, uniques);
			translationKeys((ResourceContainer<?>) container.getChild(EAIResourceRepository.PRIVATE), keys, true, uniques);
			translationKeys((ResourceContainer<?>) container.getChild(EAIResourceRepository.PROTECTED), keys, true, uniques);
		}
		if (provider instanceof WebFragmentProvider) {
			List<WebFragment> webFragments = ((WebFragmentProvider) provider).getWebFragments();
			if (webFragments != null) {
				for (WebFragment fragment : webFragments) {
					if (fragment instanceof Artifact) {
						translationKeys(fragment, keys, uniques);
					}
				}
			}
		}
	}
	
	private void translationKeys(ResourceContainer<?> container, List<KeyValuePair> keys, boolean recursive, List<String> uniques) throws IOException {
		if (container != null) {
			for (Resource resource : container) {
				if (resource instanceof ReadableResource && resource.getName().matches(".*\\.(tpl|js|css|gcss|glue|json)")) {
					ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
					try {
						byte[] bytes = IOUtils.toBytes(readable);
						String source = new String(bytes, "UTF-8");
						for (String key : ImperativeSubstitutor.getValues("%", source)) {
							String category = null;
							if (key.matches("(?s)^(?:.*?::|[a-zA-Z0-9.]+:).*")) {
								category = key.replaceAll("(?s)^(?:(.*?)::|([a-zA-Z0-9.]+):).*", "$1$2");
								key = key.replaceAll("(?s)^(?:.*?::|[a-zA-Z0-9.]+:)(.*)", "$1");
							}
							String unique = category + "::" + key;
							if (!uniques.contains(unique)) {
								uniques.add(unique);
								keys.add(new PropertyImpl(category, key));
							}
						}
					}
					finally {
						readable.close();
					}
				}
				if (recursive && resource instanceof ResourceContainer) {
					translationKeys((ResourceContainer<?>) resource, keys, recursive, uniques);
				}
			}
		}
	}
	
	@WebResult(name = "information")
	public WebApplicationInformation information(@NotNull @WebParam(name = "webApplicationId") String id) throws IOException {
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				return WebApplicationUtils.getInformation(resolved);
			}
		}
		return null;
	}
	
	@WebResult(name = "applications")
	public List<WebApplicationInformation> list(@WebParam(name = "id") String id, @WebParam(name = "realm") String realm) {
		List<WebApplicationInformation> results = new ArrayList<WebApplicationInformation>();
		List<WebApplication> artifacts = EAIResourceRepository.getInstance().getArtifacts(WebApplication.class);
		if (artifacts != null) {
			for (WebApplication application : artifacts) {
				if (id != null && !application.getId().equals(id) && !application.getId().startsWith(id + ".")) {
					continue;
				}
				if (realm != null && !realm.equals(application.getRealm())) {
					continue;
				}
				results.add(WebApplicationUtils.getInformation(application));
			}
		}
		return results;
	}
	
	public void set(@NotNull @WebParam(name = "webApplicationId") String id, @WebParam(name = "properties") List<KeyValuePair> properties) {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Can not find web application with id: " + id);
		}
		if (properties != null) {
			for (KeyValuePair property : properties) {
				resolved.getListener().getEnvironment().getParameters().put(property.getKey(), property.getValue());
			}
		}
	}

	@WebResult(name = "resources")
	public List<String> resources(@NotNull @WebParam(name = "webApplicationId") String id, @WebParam(name = "regex") final String regex) {
		List<String> resources = new ArrayList<String>();
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved != null) {
			for (ResourceContainer<?> root : resolved.getResourceHandler().getRoots()) {
				List<String> find = find(root, new ResourceFilter() {
					@Override
					public boolean accept(Resource resource) {
						return !(resource instanceof ResourceContainer) && (regex == null || resource.getName().matches(regex));
					}
				}, true, null);
				for (String resource : find) {
					resources.add("resources/" + resource);
				}
			}
		}
		return resources;
	}

	public static List<String> find(ResourceContainer<?> container, ResourceFilter filter, boolean recursive, String path) {
		List<String> result = new ArrayList<String>();
		for (Resource child : container) {
			String childPath = path == null ? child.getName() : path + "/" + child.getName();
			if (filter.accept(child))
				result.add(childPath);
			if (recursive && child instanceof ResourceContainer)
				result.addAll(find((ResourceContainer<?>) child, filter, recursive, childPath));
		}
		return result;
	}
	
	@WebResult(name = "configuration")
	public Object configuration(@NotNull @WebParam(name = "webApplicationId") String id, @NotNull @WebParam(name = "typeId") String typeId, @WebParam(name = "path") String path) throws IOException {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find web application: " + id);
		}
		DefinedType resolve = executionContext.getServiceContext().getResolver(DefinedType.class).resolve(typeId);
		if (!(resolve instanceof ComplexType)) {
			throw new IllegalArgumentException("Not a valid complex type: " + typeId);
		}
		return resolved.getConfigurationFor(path == null ? ".*" : path, (ComplexType) resolve);
	}
	
	public void configure(@NotNull @WebParam(name = "webApplicationId") String id, @NotNull @WebParam(name = "configuration") Object configuration, @WebParam(name = "path") String path, @WebParam(name = "environmentSpecific") Boolean environmentSpecific) throws IOException {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find web application: " + id);
		}
		resolved.putConfiguration(configuration, path, environmentSpecific != null && environmentSpecific);
		resolved.save(resolved.getDirectory());
		// reload the artifact
		//EAIResourceRepository.getInstance().reload(resolved.getId());
		
		ResourceEvent event = new ResourceEvent();
		event.setArtifactId(resolved.getId());
		event.setState(ResourceState.UPDATE);
		EAIResourceRepository.getInstance().getEventDispatcher().fire(event, this);
	}

	@WebResult(name = "has")
	public boolean hasFragment(@NotNull @WebParam(name = "webApplicationId") String id, @NotNull @WebParam(name = "fragmentId") String fragmentId, @WebParam(name = "active") Boolean active) {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find web application: " + id);
		}
		return hasFragment(resolved, resolved, fragmentId, active, null);
	}

	private boolean hasFragment(WebApplication application, WebFragmentProvider provider, String fragmentId, Boolean active, String path) {
		if (provider.getWebFragments() != null) {
			for (WebFragment fragment : provider.getWebFragments()) {
				if (fragment != null && fragmentId.equals(fragment.getId())) {
					// we don't care if it is active or inactive, just that it is mounted
					if (active == null) {
						return true;
					}
					else {
						boolean isActive = fragment.isStarted(application, path);
						if (active) {
							return isActive;
						}
						else {
							return !isActive;
						}
					}
				}
				else if (fragment instanceof WebFragmentProvider) {
					String childPath = path;
					if (((WebFragmentProvider) fragment).getRelativePath() != null) {
						if (childPath == null) {
							childPath = "";
						}
						childPath += "/" + ((WebFragmentProvider) fragment).getRelativePath();
						childPath = childPath.replaceAll("[/]{2,}", "/");
					}
					boolean result = hasFragment(application, (WebFragmentProvider) fragment, fragmentId, active, childPath);
					if (result) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void setSession(@NotNull @WebParam(name = "key") String key, @WebParam(name = "value") Object value) {
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		if (runtime == null) {
			throw new IllegalStateException("No runtime found");
		}
		Object session = runtime.getContext().get("session");
		if (!(session instanceof Session)) {
			throw new IllegalStateException("Could not find session");
		}
		((Session) session).set(key, value);
	}
	
	@WebResult(name = "value")
	public Object getSession(@NotNull @WebParam(name = "key") String key) {
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		if (runtime == null) {
			throw new IllegalStateException("No runtime found");
		}
		Object session = runtime.getContext().get("session");
		if (!(session instanceof Session)) {
			throw new IllegalStateException("Could not find session");
		}
		return ((Session) session).get(key);
	}
	
	@WebResult(name = "supportedLanguages")
	public List<String> supportedLanguages(@WebParam(name = "id") String id) {
		WebApplication resolved = id == null ? null : executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved != null) {
			LanguageProvider languageProvider = resolved.getLanguageProvider();
			if (languageProvider != null) {
				return languageProvider.getSupportedLanguages();
			}
		}
		return null;
	}
	
	@WebResult(name = "authentication")
	public TemporaryAuthentication newTemporaryAuthentication(@NotNull @WebParam(name = "webApplicationId") String webApplicationId, @NotNull @WebParam(name = "realm") String realm, @NotNull @WebParam(name = "alias") String alias,
				@WebParam(name = "maxUses") Integer maxUses, @WebParam(name = "until") Date until) throws IOException {
		WebApplication resolved = webApplicationId == null ? null : executionContext.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);
		if (resolved != null) {
			TemporaryAuthenticationGenerator temporaryAuthenticationGenerator = resolved.getTemporaryAuthenticationGenerator();
			if (temporaryAuthenticationGenerator != null) {
				return temporaryAuthenticationGenerator.generate(realm, alias, maxUses, until);
			}
		}
		return null;
	}
	
}
