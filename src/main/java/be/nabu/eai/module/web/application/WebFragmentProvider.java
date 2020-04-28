package be.nabu.eai.module.web.application;

import java.util.ArrayList;
import java.util.List;

public interface WebFragmentProvider {
	public List<WebFragment> getWebFragments();
	public String getRelativePath();
	// the tags from the provider itself
	public default List<String> getProviderTags() {
		return new ArrayList<String>();
	}
}
