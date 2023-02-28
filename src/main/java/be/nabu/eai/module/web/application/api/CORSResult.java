package be.nabu.eai.module.web.application.api;

import java.util.List;

public interface CORSResult {
	// the methods that are allowed
	public List<String> getMethods();
	// whether this origin is allowed
	public Boolean getAllowed();
	// credentials are cookies, authorization headers, or TLS client certificates
	public Boolean getAllowedCredentials();
	// if the above is true, are all origins allowed or just this one?
	public Boolean getAllowedAll();
}
