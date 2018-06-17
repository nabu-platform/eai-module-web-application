package be.nabu.eai.module.web.application.resource;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceResolver;

public class WebApplicationResourceResolver implements ResourceResolver {

	private static List<String> defaultSchemes = Arrays.asList(new String [] { "dhttp" });

	@Override
	public Resource getResource(URI uri, Principal principal) {
		return new WebApplicationResource(uri);
	}

	@Override
	public List<String> getDefaultSchemes() {
		return defaultSchemes;
	}

}
