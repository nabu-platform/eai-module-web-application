package be.nabu.eai.module.web.application.rate;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.repository.api.ListableSinkProviderArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "rateLimiter")
public class RateLimiterConfiguration {

	private DefinedService rateLimitSettingsProvider;
	private ListableSinkProviderArtifact database;

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.web.application.api.RateLimitSettingsProvider.settings")
	public DefinedService getRateLimitSettingsProvider() {
		return rateLimitSettingsProvider;
	}
	public void setRateLimitSettingsProvider(DefinedService rateLimitSettingsProvider) {
		this.rateLimitSettingsProvider = rateLimitSettingsProvider;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public ListableSinkProviderArtifact getDatabase() {
		return database;
	}
	public void setDatabase(ListableSinkProviderArtifact database) {
		this.database = database;
	}
	
}
