package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.module.web.application.WebConfiguration.WebConfigurationPart;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.validator.api.Validation;

public class WebApplicationManager extends JAXBArtifactManager<WebApplicationConfiguration, WebApplication> {

	public WebApplicationManager() {
		super(WebApplication.class);
	}

	@Override
	protected WebApplication newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WebApplication(id, container, repository);
	}

	@Override
	public List<String> getReferences(WebApplication artifact) throws IOException {
		List<String> references = new ArrayList<String>();
		List<String> superReferences = super.getReferences(artifact);
		if (superReferences != null) {
			references.addAll(superReferences);
		}
		WebConfiguration fragmentConfiguration = artifact.getFragmentConfiguration();
		if (fragmentConfiguration != null && fragmentConfiguration.getParts() != null) {
			for (WebConfigurationPart part : fragmentConfiguration.getParts()) {
				if (part != null && part.getType() != null && !references.contains(part.getType())) {
					references.add(part.getType());
				}
			}
		}
		return references;
	}

	@Override
	public List<Validation<?>> updateReference(WebApplication artifact, String from, String to) throws IOException {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		List<Validation<?>> superMessages = super.updateReference(artifact, from, to);
		if (superMessages != null) {
			messages.addAll(superMessages);
		}
		WebConfiguration fragmentConfiguration = artifact.getFragmentConfiguration();
		if (fragmentConfiguration != null && fragmentConfiguration.getParts() != null) {
			for (WebConfigurationPart part : fragmentConfiguration.getParts()) {
				if (part != null && part.getType() != null && part.getType().equals(from)) {
					part.setType(to);
				}
			}
		}
		return messages;
	}
	
}
