package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import be.nabu.libs.authentication.api.Permission;

//belatedly realized that to register a web fragment provider, it _must_ be a fragment itself
//however, if you just want to offer multiple web fragments, you don't want to reimplement everything every time
public interface MountableWebFragmentProvider extends WebFragmentProvider, WebFragment {
	
	@Override
	public default void start(WebApplication artifact, String path) throws IOException {
		List<WebFragment> webFragments = getWebFragments();
		if (webFragments != null) {
			if (getRelativePath() != null) {
				path += "/" + getRelativePath();
				path = path.replaceAll("[/]{2,}", "/");
			}
			// new list so we don't affect the original order
			webFragments = new ArrayList<WebFragment>(webFragments);
			Collections.sort(webFragments, new Comparator<WebFragment>() {
				@Override
				public int compare(WebFragment o1, WebFragment o2) {
					if (o1 == null) {
						return 1;
					}
					else if (o2 == null) {
						return -1;
					}
					else {
						return o1.getPriority().compareTo(o2.getPriority());
					}
				}
			});
			for (WebFragment fragment : webFragments) {
				if (fragment != null) {
					fragment.start(artifact, path);
				}
			}
		}
	}
	
	@Override
	public default void stop(WebApplication artifact, String path) {
		List<WebFragment> webFragments = getWebFragments();
		if (webFragments != null) {
			if (getRelativePath() != null) {
				path += "/" + getRelativePath();
				path = path.replaceAll("[/]{2,}", "/");
			}
			for (WebFragment fragment : webFragments) {
				if (fragment != null) {
					fragment.stop(artifact, path);
				}
			}
		}
	}
	
	@Override
	public default List<Permission> getPermissions(WebApplication artifact, String path) {
		try {
			if (getRelativePath() != null) {
				path += "/" + getRelativePath();
				path = path.replaceAll("[/]{2,}", "/");
			}
			List<Permission> permissions = new ArrayList<Permission>();
			List<WebFragment> webFragments = getWebFragments();
			if (webFragments != null) {
				for (WebFragment fragment : webFragments) {
					if (fragment != null) {
						List<Permission> fragmentPermissions = fragment.getPermissions(artifact, path);
						if (fragmentPermissions != null) {
							permissions.addAll(fragmentPermissions);
						}
					}
				}
			}
			return permissions;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public default boolean isStarted(WebApplication artifact, String path) {
		return true;
	}
}
