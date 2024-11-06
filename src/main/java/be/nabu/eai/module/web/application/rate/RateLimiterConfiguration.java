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
