package be.nabu.eai.module.web.application;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class WebApplicationManager extends JAXBArtifactManager<WebApplicationConfiguration, WebApplication> {

	public WebApplicationManager() {
		super(WebApplication.class);
	}

	@Override
	protected WebApplication newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WebApplication(id, container, repository);
	}

}
