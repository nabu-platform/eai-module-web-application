package nabu.web.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import nabu.web.application.types.PropertyImpl;
import nabu.web.application.types.WebApplicationInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.glue.ScriptUtils;
import be.nabu.glue.api.Script;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.ResourceFilter;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.utils.io.IOUtils;

@WebService
public class Services {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private ExecutionContext executionContext;
	
	@WebResult(name = "translationKeys")
	public List<KeyValuePair> translationKeys(@NotNull @WebParam(name = "artifactId") String id) {
		List<KeyValuePair> properties = new ArrayList<KeyValuePair>();
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				Pattern pattern = Pattern.compile("%\\{([^}]+)\\}");
				for (Script script : resolved.getListener().getRepository()) {
					String fullName = ScriptUtils.getFullName(script);
					try {
						InputStream input = script.getSource();
						try {
							byte[] bytes = IOUtils.toBytes(IOUtils.wrap(input));
							String source = new String(bytes, script.getCharset());
							Matcher matcher = pattern.matcher(source);
							while(matcher.find()) {
								String key = matcher.group(1);
								properties.add(new PropertyImpl("page:" + fullName, key));
							}
						}
						finally {
							input.close();
						}
					}
					catch (IOException e) {
						logger.error("Could not load source code for script: " + fullName);
					}
				}
			}
			else {
				throw new IllegalArgumentException("Can not find web artifact: " + id);
			}
		}
		return properties;
	}
	
	@WebResult(name = "information")
	public WebApplicationInformation information(@NotNull @WebParam(name = "webApplicationId") String id) throws IOException {
		if (id != null) {
			WebApplication resolved = executionContext.getServiceContext().getResolver(WebApplication.class).resolve(id);
			if (resolved != null) {
				WebApplicationInformation information = new WebApplicationInformation();
				information.setRealm(resolved.getRealm());
				information.setCharset(resolved.getConfiguration().getCharset() == null ? Charset.defaultCharset() : Charset.forName(resolved.getConfiguration().getCharset()));
				if (resolved.getConfiguration().getVirtualHost() != null) {
					information.setHost(resolved.getConfiguration().getVirtualHost().getConfiguration().getHost());
					information.setAliases(resolved.getConfiguration().getVirtualHost().getConfiguration().getAliases());
					information.setPort(resolved.getConfiguration().getVirtualHost().getConfiguration().getServer() == null ? null : resolved.getConfiguration().getVirtualHost().getConfiguration().getServer().getConfiguration().getPort());
					information.setSecure(resolved.getConfiguration().getVirtualHost().getConfiguration().getServer() == null ? null : resolved.getConfiguration().getVirtualHost().getConfiguration().getServer().getConfiguration().getKeystore() != null);
				}
				information.setPath(resolved.getConfiguration().getPath());
				Map<String, String> properties = resolved.getProperties();
				for (String key : properties.keySet()) {
					information.getProperties().add(new PropertyImpl(key, properties.get(key)));
				}
				return information;
			}
		}
		return null;
	}

	@WebResult(name = "resources")
	public List<String> resources(@NotNull @WebParam(name = "artifactId") String id, @WebParam(name = "regex") final String regex) {
		List<String> resources = new ArrayList<String>();
		if (id != null) {
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

}
