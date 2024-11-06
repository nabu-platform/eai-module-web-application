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
