package be.nabu.eai.module.web.application;

import java.util.List;

public interface WebFragmentProvider {
	public List<WebFragment> getWebFragments();
	public String getRelativePath();
}
