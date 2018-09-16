package nabu.web.application.types;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import be.nabu.libs.types.api.KeyValuePair;

@XmlRootElement(name = "webApplication")
@XmlType(propOrder = { "id", "realm", "path", "root", "scheme", "charset", "host", "aliases", "port", "secure", "translationService", "scriptCacheProviderId", "properties" })
public class WebApplicationInformation {
	// root is slightly different from path: path is the actual configured path
	// root is the interpreted path guaranteed to end in a "/" for concatenation
	private String id, realm, path, scheme, root;
	private Charset charset;
	private String host;
	private List<String> aliases;
	private Integer port;
	private Boolean secure;
	private String translationService;
	private String scriptCacheProviderId;
	private List<KeyValuePair> properties;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getRealm() {
		return realm;
	}
	public void setRealm(String realm) {
		this.realm = realm;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Charset getCharset() {
		return charset;
	}
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public Boolean getSecure() {
		return secure;
	}
	public void setSecure(Boolean secure) {
		this.secure = secure;
	}
	public List<KeyValuePair> getProperties() {
		if (properties == null) {
			properties = new ArrayList<KeyValuePair>();
		}
		return properties;
	}
	public void setProperties(List<KeyValuePair> properties) {
		this.properties = properties;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public List<String> getAliases() {
		return aliases;
	}
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	public String getTranslationService() {
		return translationService;
	}
	public void setTranslationService(String translationService) {
		this.translationService = translationService;
	}
	public String getScriptCacheProviderId() {
		return scriptCacheProviderId;
	}
	public void setScriptCacheProviderId(String scriptCacheProviderId) {
		this.scriptCacheProviderId = scriptCacheProviderId;
	}
	public String getScheme() {
		return scheme;
	}
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	public String getRoot() {
		if (root == null) {
			String path = getPath();
			if (path == null) {
				path = "/";
			}
			if (!path.endsWith("/")) {
				path += "/";
			}
			root = path;
		}
		return root;
	}
	public void setRoot(String root) {
		this.root = root;
	}
}