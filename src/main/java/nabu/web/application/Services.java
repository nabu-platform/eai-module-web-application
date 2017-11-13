package nabu.web.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import nabu.web.application.types.PropertyImpl;
import nabu.web.application.types.WebApplicationInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationUtils;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.application.WebFragmentProvider;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.glue.impl.ImperativeSubstitutor;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.ResourceFilter;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

@WebService
public class Services {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private ExecutionContext executionContext;
	
	@WebResult(name = "permissions")
	public List<Permission> permissions(@NotNull @WebParam(name = "webApplicationId") String id) {
		List<Permission> permissions = new ArrayList<Permission>();
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				permissions(resolved, permissions);
			}
		}
		return permissions;
	}
	
	private void permissions(WebApplication application, List<Permission> permissions) {
		String path = application.getConfig().getPath();
		if (path == null) {
			path = "/";
		}
		for (WebFragment fragment : application.getWebFragments()) {
			List<Permission> fragmentPermissions = fragment.getPermissions(application, path);
			if (fragmentPermissions != null) {
				permissions.addAll(fragmentPermissions);
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
				if (resource instanceof ReadableResource && resource.getName().matches(".*\\.(tpl|js|css|gcss|glue)")) {
					ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
					try {
						byte[] bytes = IOUtils.toBytes(readable);
						String source = new String(bytes, "UTF-8");
						for (String key : ImperativeSubstitutor.getValues("%", source)) {
							String category = null;
							if (key.matches("^[\\w.]+:.*")) {
								category = key.replaceAll("^([\\w.]+):.*", "$1");
								key = key.substring(category.length() + 1);
							}
							String unique = category + ":" + key;
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

	@WebResult(name = "has")
	public boolean hasFragment(@NotNull @WebParam(name = "webApplicationId") String id, @NotNull @WebParam(name = "fragmentId") String fragmentId) {
		WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
		if (resolved == null) {
			throw new IllegalArgumentException("Could not find web application: " + id);
		}
		return hasFragment(resolved, fragmentId);
	}

	private boolean hasFragment(WebFragmentProvider provider, String fragmentId) {
		if (provider.getWebFragments() != null) {
			for (WebFragment fragment : provider.getWebFragments()) {
				if (fragment != null && fragmentId.equals(fragment.getId())) {
					return true;
				}
				else if (fragment instanceof WebFragmentProvider) {
					boolean result = hasFragment((WebFragmentProvider) fragment, fragmentId);
					if (result) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
