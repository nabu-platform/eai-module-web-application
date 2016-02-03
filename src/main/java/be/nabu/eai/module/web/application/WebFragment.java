package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.util.List;

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
	 * Is it running on this web artifact?
	 */
	public boolean isStarted(WebApplication artifact, String path);
}
