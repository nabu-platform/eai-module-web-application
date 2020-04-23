package be.nabu.eai.module.web.application.api;

import java.util.Date;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;

// if you want to limit on for example an API key, we currently assume you first resolve that API key to a valid token, you then get the token for rate limiting...
// in the future we might add rate limiting to non-web related things, this interface is ready for that
// for instance one might imagine a shared environment where we limit amount of service executions? or you pay per execution or...
public interface RateLimitProvider {
	@WebResult(name = "settings")
	public List<RateLimitSettings> settings(
			@NotNull @WebParam(name = "applicationId") String applicationId,
			@NotNull @WebParam(name = "source") Source source, 
			@WebParam(name = "token") Token token, 
			@WebParam(name = "device") Device device, 
			@WebParam(name = "action") String action, 
			@WebParam(name = "context") String context);
	
	@WebResult(name = "rateLimit")
	public RateLimitCheck check(
			@NotNull @WebParam(name = "applicationId") String applicationId,
			@NotNull @WebParam(name = "ruleId") String ruleId,
			@WebParam(name = "identity") String identity, 
			@WebParam(name = "context") String context, 
			@WebParam(name = "since") Date date);
	
	@WebResult(name = "rateLimit")
	public void log(
			@NotNull @WebParam(name = "applicationId") String applicationId,
			@NotNull @WebParam(name = "ruleId") String ruleId,
			@WebParam(name = "identity") String identity, 
			@WebParam(name = "context") String context, 
			@NotNull @WebParam(name = "occurred") Date date);
}
