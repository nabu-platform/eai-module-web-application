package be.nabu.eai.module.web.application;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.util.KeyValueMapAdapter;

@XmlRootElement(name = "webFragmentConfigurations")
public class WebConfiguration {
	
	private List<WebConfigurationPart> parts;
	
	public List<WebConfigurationPart> getParts() {
		if (parts == null) {
			parts = new ArrayList<WebConfigurationPart>();
		}
		return parts;
	}
	public void setParts(List<WebConfigurationPart> parts) {
		this.parts = parts;
	}

	public static class WebConfigurationPart {
		private String type;
		// the paths for which this configuration counts, null means all
		// the most specific one wins
		private String pathRegex;
		// this is deprecated but still provides some documentational support
		private List<String> paths;
		private Map<String, String> configuration;
		// whether or not the given configuration is environment specific (almost nothing is)
		private Boolean environmentSpecific;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public List<String> getPaths() {
			if (paths == null) {
				paths = new ArrayList<String>();
			}
			return paths;
		}
		public void setPaths(List<String> paths) {
			this.paths = paths;
		}
		@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
		public Map<String, String> getConfiguration() {
			if (configuration == null) {
				configuration = new HashMap<String, String>();
			}
			return configuration;
		}
		public void setConfiguration(Map<String, String> configuration) {
			this.configuration = configuration;
		}
		public String getPathRegex() {
			return pathRegex;
		}
		public void setPathRegex(String pathRegex) {
			this.pathRegex = pathRegex;
		}
		public Boolean getEnvironmentSpecific() {
			return environmentSpecific;
		}
		public void setEnvironmentSpecific(Boolean environmentSpecific) {
			this.environmentSpecific = environmentSpecific;
		}
	}
	
	public static WebConfiguration unmarshal(InputStream input) {
		try {
			return (WebConfiguration) JAXBContext.newInstance(WebConfiguration.class).createUnmarshaller().unmarshal(input);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void marshal(OutputStream output) {
		try {
			Marshaller marshaller = JAXBContext.newInstance(WebConfiguration.class).createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(this, output);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
}
