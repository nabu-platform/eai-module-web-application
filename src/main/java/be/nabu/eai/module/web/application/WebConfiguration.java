package be.nabu.eai.module.web.application;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
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
		private List<String> paths;
		private Map<String, String> configuration;
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
			JAXBContext.newInstance(WebConfiguration.class).createMarshaller().marshal(this, output);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
}
