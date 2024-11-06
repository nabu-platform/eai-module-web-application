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

package nabu.web.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.web.application.RateLimitImpl;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationUtils;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.application.WebFragmentProvider;
import be.nabu.eai.module.web.application.api.PermissionWithRole;
import be.nabu.eai.module.web.application.api.RESTFragment;
import be.nabu.eai.module.web.application.api.RateLimit;
import be.nabu.eai.module.web.application.api.TemporaryAuthentication;
import be.nabu.eai.module.web.application.api.TemporaryAuthenticationGenerator;
import be.nabu.eai.module.web.application.api.TemporaryAuthenticationRevoker;
import be.nabu.eai.module.web.application.api.TemporaryAuthenticator;
import be.nabu.eai.module.web.application.api.TranslationKey;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.LanguageProvider;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.events.ResourceEvent;
import be.nabu.eai.repository.events.ResourceEvent.ResourceState;
import be.nabu.glue.impl.ImperativeSubstitutor;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.impl.BasicPrincipalWithDeviceImpl;
import be.nabu.libs.authentication.impl.PermissionImpl;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.core.SameSite;
import be.nabu.libs.nio.impl.RequestProcessor;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.ResourceFilter;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceDescription;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.libs.types.base.Duration;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiableHeader;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeUtils;
import nabu.web.application.types.Cookie;
import nabu.web.application.types.TranslationKeyImpl;
import nabu.web.application.types.WebApplicationInformation;
import nabu.web.application.types.WebFragmentInformation;

@WebService
public class Services {
	private ExecutionContext executionContext;

	@WebResult(name = "cookie")
	public String formatCookie(@WebParam(name = "webApplicationId") @NotNull String id, @WebParam(name = "key") String key, @WebParam(name = "value") String value, @WebParam(name = "expires") Date expires, @WebParam(name = "sameSite") SameSite sameSite) {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (sameSite == null) {
			sameSite = resolved.getConfig().getDefaultCookieSitePolicy();
		}
		ModifiableHeader newSetCookieHeader = HTTPUtils.newSetCookieHeader(key, value, expires, resolved.getCookiePath(), null, resolved.isSecure(), true, sameSite);
		return MimeUtils.getFullHeaderValue(newSetCookieHeader);
	}
	
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

	@WebResult(name = "sessionId")
	public String getSessionId() {
		Object currentRequest = RequestProcessor.getCurrentRequest();
		if (currentRequest instanceof HTTPRequest) {
			ModifiablePart content = ((HTTPRequest) currentRequest).getContent();
			if (content != null) {
				Header header = MimeUtils.getHeader("Session-Id", content.getHeaders());
				if (header != null) {
					return header.getValue();
				}
			}
		}
		return null;
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
		// the permission needed to update the service context via header
		permissions.add(new PermissionImpl("$application.setServiceContext", "=\"webApplication:\" + input/webApplicationId"));
		return permissions;
	}
	
	@WebResult(name = "rateLimits")
	public List<RateLimit> rateLimits(@NotNull @WebParam(name = "webApplicationId") String id) {
		List<RateLimit> rateLimits = new ArrayList<RateLimit>();
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				rateLimits(resolved, rateLimits);
			}
		}
		return rateLimits;
	}
	
	// get the test roles for this application
	public List<String> testRoles(@NotNull @WebParam(name = "webApplicationId") String id) {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Invalid web application: " + id);
		}
		return resolved.getConfig().getTestRole();
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
	
	public boolean hasAnyRole(@NotNull @WebParam(name = "webApplicationId") String id, @WebParam(name = "token") Token token, @WebParam(name = "role") List<String> role) throws IOException {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Invalid web application: " + id);
		}
		if (resolved.getRoleHandler() == null) {
			return true;
		}
		else if (role != null) {
			for (String single : role) {
				if (resolved.getRoleHandler().hasRole(token, single)) {
					return true;
				}
			}
		}
		return false;
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
		// add the permissions for the fragments themselves
		List<RESTFragment> fragments = application.getFragments(false, null);
		for (RESTFragment fragment : fragments) {
			if (fragment.getPermissionAction() != null) {
				permissions.add(new PermissionImpl(fragment.getPermissionAction(), fragment.getPermissionContext()));
			}
		}
		// don't recurse, the web fragment providers should do that themselves
	}
	
	private void rateLimits(WebApplication application, List<RateLimit> rateLimits) {
		String path = application.getConfig().getPath();
		if (path == null) {
			path = "/";
		}
		for (WebFragment fragment : application.getWebFragments()) {
			if (fragment != null) {
				List<RateLimit> fragmentRateLimits = fragment.getRateLimits(application, path);
				if (fragmentRateLimits != null) {
					rateLimits.addAll(fragmentRateLimits);
				}
			}
		}
		// add the permissions for the fragments themselves
		List<RESTFragment> fragments = application.getFragments(false, null);
		for (RESTFragment fragment : fragments) {
			if (fragment.getRateLimitAction() != null) {
				rateLimits.add(new RateLimitImpl(fragment.getRateLimitAction(), fragment.getRateLimitContext()));
			}
		}
		// don't recurse, the web fragment providers should do that themselves
	}
	
	@WebResult(name = "translationKeys")
	public List<TranslationKey> translationKeys(@NotNull @WebParam(name = "webApplicationId") String id) throws IOException {
		List<TranslationKey> properties = new ArrayList<TranslationKey>();
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				Map<String, TranslationKey> keys = new HashMap<String, TranslationKey>();
				translationKeys(resolved, keys);
				properties.addAll(keys.values());
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
	
	private void translationKeys(Artifact provider, Map<String, TranslationKey> keys) throws IOException {
		Entry entry = EAIResourceRepository.getInstance().getEntry(provider.getId());
		if (entry instanceof ResourceEntry) {
			ResourceContainer<?> container = ((ResourceEntry) entry).getContainer();
			translationKeys(container, keys, false, null);
			translationKeys((ResourceContainer<?>) container.getChild(EAIResourceRepository.PUBLIC), keys, true, "public");
			translationKeys((ResourceContainer<?>) container.getChild(EAIResourceRepository.PRIVATE), keys, true, "private");
			translationKeys((ResourceContainer<?>) container.getChild(EAIResourceRepository.PROTECTED), keys, true, "protected");
		}
		if (provider instanceof WebFragmentProvider) {
			List<WebFragment> webFragments = ((WebFragmentProvider) provider).getWebFragments();
			if (webFragments != null) {
				for (WebFragment fragment : webFragments) {
					if (fragment instanceof Artifact) {
						translationKeys(fragment, keys);
					}
				}
			}
		}
	}
	
	@WebResult(name = "substitutions")
	public List<String> substitutions(@WebParam(name = "content") String content, @WebParam(name = "separator") String separator) {
		return ImperativeSubstitutor.getValues(separator, content);
	}
	
	private void translationKeys(ResourceContainer<?> container, Map<String, TranslationKey> keys, boolean recursive, String path) throws IOException {
		if (container != null) {
			for (Resource resource : container) {
				String childPath = path == null ? resource.getName() : path + "/" + resource.getName();
				// we don't want to scrape the cache!
				if (childPath.equals("private/cache")) {
					continue;
				}
				// @2021-04-16: added "eglue" which was curiously missing from the list of extensions
				// it is no longer clear whether this was an active design decision or an oversight
				// added for OV where eglue is still used a lot, if this breaks somehow, might need to revert and find another solution for OV
				if (resource instanceof ReadableResource && resource.getName().matches(".*\\.(tpl|js|css|gcss|glue|json|eglue|scss|sass)")) {
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
							if (!keys.containsKey(unique)) {
								TranslationKeyImpl translationKey = new TranslationKeyImpl(category, key);
								translationKey.setFiles(new ArrayList<String>());
								keys.put(unique, translationKey);
							}
							// if the page is not already in the list, add it
							if (!keys.get(unique).getFiles().contains(childPath)) {
								keys.get(unique).getFiles().add(childPath);
							}
						}
					}
					finally {
						readable.close();
					}
				}
				if (recursive && resource instanceof ResourceContainer) {
					translationKeys((ResourceContainer<?>) resource, keys, recursive, childPath);
				}
			}
		}
	}
	
	@WebResult(name = "current")
	public WebApplicationInformation current() throws IOException {
		ServiceRuntime runtime = ServiceRuntime.getRuntime();
		String id = runtime == null ? null : (String) runtime.getContext().get("webApplicationId");
		if (id != null) {
			WebApplicationInformation information = information(id);
			// the id is not a web application :(
			if (information == null) {
				throw new IllegalStateException("Could not find valid web application wrapper");
			}
			return information;
		}
		else {
			throw new IllegalStateException("Could not find valid web application wrapper");
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

	@ServiceDescription(comment = "List all the static resources in a web application that match the given regex")
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
	
	@ServiceDescription(comment = "List all the files in a web application that match the given regex")
	public List<String> files(@NotNull @WebParam(name = "webApplicationId") String id, @WebParam(name = "regex") final String regex, @WebParam(name = "includeFragments") Boolean includeFragments) throws IOException {
		List<String> resources = new ArrayList<String>();
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved != null) {
			resources.addAll(providedFolders(resolved.getRepository(), resolved, regex, new ArrayList<String>(), includeFragments != null && includeFragments));
		}
		return resources;
	}
	
	private List<String> providedFolders(Repository repository, WebFragmentProvider provider, String regex, List<String> blacklist, boolean includeFragments) throws IOException {
		List<String> files = new ArrayList<String>();
		if (provider.getWebFragments() != null && includeFragments) {
			for (WebFragment fragment : provider.getWebFragments()) {
				if (fragment instanceof WebFragmentProvider) {
					if (blacklist.contains(fragment.getId())) {
						continue;
					}
					else {
						blacklist.add(fragment.getId());
					}
					files.addAll(providedFolders(repository, (WebFragmentProvider) fragment, regex, blacklist, includeFragments));
				}
			}
		}
		if (provider instanceof Artifact) {
			Entry entry = repository.getEntry(((Artifact) provider).getId());
			if (entry instanceof ResourceEntry) {
				ResourceContainer<?> container = ((ResourceEntry) entry).getContainer();
				List<String> find = find(container, new ResourceFilter() {
					@Override
					public boolean accept(Resource resource) {
						return resource.getName().matches(regex);
					}
				}, true, null);
				for (String resource : find) {
					files.add("repository:" + ((Artifact) provider).getId() + ":/" + resource);
				}
			}
		}
		return files;
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
		resolved.putConfiguration(configuration, path, environmentSpecific);
		resolved.save(resolved.getDirectory());
		// reload the artifact
		//EAIResourceRepository.getInstance().reload(resolved.getId());
		
		ResourceEvent event = new ResourceEvent();
		event.setArtifactId(resolved.getId());
		event.setState(ResourceState.UPDATE);
		EAIResourceRepository.getInstance().getEventDispatcher().fire(event, this);
	}

	@WebResult(name = "fragment")
	public WebFragmentInformation fragment(@NotNull @WebParam(name = "webApplicationId") String id, @NotNull @WebParam(name = "fragmentId") String fragmentId) {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find web application: " + id);
		}
		return fragment(resolved, resolved, fragmentId, null);
	}
	private WebFragmentInformation fragment(WebApplication application, WebFragmentProvider provider, String fragmentId, String path) {
		if (provider.getWebFragments() != null) {
			String relativePath = provider.getRelativePath();
			if (relativePath == null) {
				relativePath = "";
			}
			else {
				relativePath = relativePath.replaceAll("^[/]+", "");
			}
			String fragmentProviderPath = (path == null ? "/" : path + "/") + relativePath;
			for (WebFragment fragment : provider.getWebFragments()) {
				if (fragment != null && fragmentId.equals(fragment.getId())) {
					WebFragmentInformation information = new WebFragmentInformation();
					information.setActive(fragment.isStarted(application, path));
					// we always want to end in a "/" to be predictable
					information.setPath((fragmentProviderPath + "/").replaceAll("[/]{2,}", "/"));
					return information;
				}
				else if (fragment instanceof WebFragmentProvider) {
					WebFragmentInformation potential = fragment(application, (WebFragmentProvider) fragment, fragmentId, fragmentProviderPath);
					if (potential != null) {
						return potential;
					}
				}
			}
		}
		return null;
	}
	
	@ServiceDescription(comment = "Check if {webApplicationId|a web application} has {fragmentId|a web fragment}")
	@WebResult(name = "has")
	public boolean hasFragment(@NotNull @WebParam(name = "webApplicationId") String id, @NotNull @WebParam(name = "fragmentId") String fragmentId, @WebParam(name = "active") Boolean active) {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find web application: " + id);
		}
		return hasFragment(resolved, resolved, fragmentId, active, null);
	}
	
	public static boolean hasFragment(WebApplication application, String fragmentId, Boolean active) {
		return hasFragment(application, application, fragmentId, active, null);
	}

	private static boolean hasFragment(WebApplication application, WebFragmentProvider provider, String fragmentId, Boolean active, String path) {
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
	
	public void revokeTemporaryAuthentication(@NotNull @WebParam(name = "webApplicationId") String webApplicationId, @NotNull @WebParam(name = "tokenId") String tokenId) throws IOException {
		WebApplication resolved = webApplicationId == null ? null : executionContext.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);
		if (resolved != null) {
			TemporaryAuthenticationRevoker temporaryAuthenticationRevoker = resolved.getTemporaryAuthenticationRevoker();
			if (temporaryAuthenticationRevoker != null) {
				temporaryAuthenticationRevoker.revoke(resolved.getId(), tokenId);
			}
		}
	}
	
	// the authentication id is meant as a generalization of "userid" which is somewhat human-specific
	@WebResult(name = "authentication")
	public TemporaryAuthentication newTemporaryAuthentication(@NotNull @WebParam(name = "webApplicationId") String webApplicationId, @NotNull @WebParam(name = "alias") String alias, @WebParam(name = "maxUses") Integer maxUses, @WebParam(name = "until") Date until,
			// you must set a reason for the temporary authentication, for example reason "execution" could be to execute a service (e.g. file download)
			// you could also set the name of a service itself to be more specific
			// reason "authentication" might be for authentication
			@NotNull @WebParam(name = "type") String type,
			// pregenerate a secret
			@WebParam(name = "secret") String secret,
			// correlate it to something of interest
			@WebParam(name = "correlationId") String correlationId,
			@WebParam(name = "timeout") Duration timeout,
			@WebParam(name = "authenticationId") String authenticationId,
			@WebParam(name = "device") Device device,
			@WebParam(name = "impersonator") String impersonator,
			@WebParam(name = "impersonatorRealm") String impersonatorRealm,
			@WebParam(name = "impersonatorId") String impersonatorId,
			@WebParam(name = "tokenId") String tokenId,
			@WebParam(name = "realm") String realm,
			@WebParam(name = "authenticator") String authenticator) throws IOException {
		WebApplication resolved = webApplicationId == null ? null : executionContext.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);
		if (resolved != null) {
			TemporaryAuthenticationGenerator temporaryAuthenticationGenerator = resolved.getTemporaryAuthenticationGenerator();
			if (temporaryAuthenticationGenerator != null) {
				return temporaryAuthenticationGenerator.generate(resolved.getId(), realm == null ? resolved.getRealm() : realm, alias, authenticationId, maxUses, until, timeout, type, secret, correlationId, device, impersonator, impersonatorRealm, impersonatorId, tokenId, authenticator);
			}
		}
		return null;
	}
	
	public void clearOptimizedCache(@NotNull @WebParam(name = "webApplicationId") String webApplicationId) throws IOException {
		WebApplication resolved = webApplicationId == null ? null : executionContext.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find application: " + webApplicationId);
		}
		resolved.clearOptimizedCache();
	}
	
	public Token temporarilyAuthenticate(@NotNull @WebParam(name = "webApplicationId") String webApplicationId, @NotNull @WebParam(name = "authentication") TemporaryAuthentication authentication, 
			@NotNull @WebParam(name = "type") String type, 
			@WebParam(name = "correlationId") String correlationId, 
			@WebParam(name = "device") Device device) throws IOException {
		WebApplication resolved = webApplicationId == null ? null : executionContext.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);
		if (resolved != null) {
			TemporaryAuthenticator temporaryAuthenticator = resolved.getTemporaryAuthenticator();
			if (temporaryAuthenticator != null) {
				return temporaryAuthenticator.authenticate(resolved.getRealm(), authentication, device, type, correlationId);
			}
		}
		return null;
	}
	
	@WebResult(name = "token")
	public Token authenticate(@NotNull @WebParam(name = "webApplicationId") String webApplicationId, @WebParam(name = "type") String type, @NotNull @WebParam(name = "alias") String alias, @NotNull @WebParam(name = "password") String password, @WebParam(name = "device") Device device) {
		WebApplication resolved = webApplicationId == null ? null : executionContext.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);
		if (resolved != null) {
			Authenticator authenticator = resolved.getAuthenticator();
			if (authenticator != null) {
				BasicPrincipalWithDeviceImpl basicPrincipalWithDeviceImpl = new BasicPrincipalWithDeviceImpl(alias, password, device);
				if (type != null) {
					basicPrincipalWithDeviceImpl.setType(type);
				}
				return authenticator.authenticate(resolved.getRealm(), basicPrincipalWithDeviceImpl);
			}
		}
		return null;
	}
}
