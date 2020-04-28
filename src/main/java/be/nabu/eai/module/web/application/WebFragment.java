package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.module.web.application.api.RateLimit;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.authentication.api.Permission;

public interface WebFragment extends Artifact {
	/**
	 * Start on the given web artifact
	 */
	public void start(WebApplication artifact, String path) throws IOException;
	/**
	 * Stop on the given web artifact
	 */
	public void stop(WebApplication artifact, String path);
	/**
	 * List the permissions that the web fragments knows about
	 */
	public List<Permission> getPermissions(WebApplication artifact, String path);
	/**
	 * List the rate limits that the web fragment knows about
	 */
	public default List<RateLimit> getRateLimits(WebApplication artifact, String path) {
		return null;
	}
	public default List<String> getTags() {
		return new ArrayList<String>();
	}
	/**
	 * Is it running on this web artifact?
	 */
	public boolean isStarted(WebApplication artifact, String path);
	
	/**
	 * The priority for this listener
	 */
	public default WebFragmentPriority getPriority() {
		return WebFragmentPriority.NORMAL;
	}
	/**
	 * Configurations for a web fragment
	 */
	public default List<WebFragmentConfiguration> getFragmentConfiguration() {
		return new ArrayList<WebFragmentConfiguration>();
	}
}
