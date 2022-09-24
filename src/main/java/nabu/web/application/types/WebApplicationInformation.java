package nabu.web.application.types;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import be.nabu.libs.types.api.KeyValuePair;

@XmlRootElement(name = "webApplication")
@XmlType(propOrder = { "id", "realm", "path", "root", "cookiePath", "scheme", "charset", "host", "aliases", "port", "secure", "translationService", "scriptCacheProviderId", "properties", "html5Mode", "errorCodes", "defaultLanguage", "lastModified", "lastCacheUpdate", "stateless", "optimizedLoad" })
public class WebApplicationInformation {
	// root is slightly different from path: path is the actual configured path
	// root is the interpreted path guaranteed to end in a "/" for concatenation
	private String id, realm, path, scheme, root, cookiePath;
	private Charset charset;
	private String host;
	private List<String> aliases;
	private Integer port;
	private Boolean secure;
	private String translationService;
	private String scriptCacheProviderId, defaultLanguage;
	private List<KeyValuePair> properties;
	private boolean html5Mode, stateless, optimizedLoad;
	private List<String> errorCodes;
	private Date lastModified, lastCacheUpdate;

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
	public boolean isHtml5Mode() {
		return html5Mode;
	}
	public void setHtml5Mode(boolean html5Mode) {
		this.html5Mode = html5Mode;
	}
	public String getCookiePath() {
		return cookiePath;
	}
	public void setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
	}
	public List<String> getErrorCodes() {
		return errorCodes;
	}
	public void setErrorCodes(List<String> errorCodes) {
		this.errorCodes = errorCodes;
	}
	public String getDefaultLanguage() {
		return defaultLanguage;
	}
	public void setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public boolean isStateless() {
		return stateless;
	}
	public void setStateless(boolean stateless) {
		this.stateless = stateless;
	}
	public boolean isOptimizedLoad() {
		return optimizedLoad;
	}
	public void setOptimizedLoad(boolean optimizedLoad) {
		this.optimizedLoad = optimizedLoad;
	}
	public Date getLastCacheUpdate() {
		return lastCacheUpdate;
	}
	public void setLastCacheUpdate(Date lastCacheUpdate) {
		this.lastCacheUpdate = lastCacheUpdate;
	}
}