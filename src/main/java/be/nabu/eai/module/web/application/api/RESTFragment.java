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

package be.nabu.eai.module.web.application.api;

import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.api.Documented;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;

public interface RESTFragment extends Artifact {
	public String getPath();
	public String getMethod();
	public List<String> getConsumes();
	public List<String> getProduces();
	public Type getInput();
	public Type getOutput();
	public List<Element<?>> getQueryParameters();
	public List<Element<?>> getHeaderParameters();
	public List<Element<?>> getPathParameters();
	
	public default Documented getDocumentation() {
		return null;
	}
	// the roles that are allowed to execute this rest fragment
	public default List<String> getAllowedRoles() {
		return null;
	}
	public default String getPermissionAction() {
		return null;
	}
	public default String getPermissionContext() {
		return null;
	}
	public default boolean isCacheable() {
		return false;
	}
	public default String getRateLimitAction() {
		return null;
	}
	public default String getRateLimitContext() {
		return null;
	}
	public default List<String> getTags() {
		return null;
	}
	public default String getSummary() {
		return null;
	}
	public default String getDescription() {
		return null;
	}
	public default Map<String, Object> getExtensions() {
		return null;
	}
}
