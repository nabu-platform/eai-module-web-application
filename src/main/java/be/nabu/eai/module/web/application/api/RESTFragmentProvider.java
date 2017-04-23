package be.nabu.eai.module.web.application.api;

import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;

public interface RESTFragmentProvider extends Artifact {
	public List<RESTFragment> getFragments();
}