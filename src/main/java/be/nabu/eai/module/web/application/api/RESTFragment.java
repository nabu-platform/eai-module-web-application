package be.nabu.eai.module.web.application.api;

import java.util.List;

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
}
