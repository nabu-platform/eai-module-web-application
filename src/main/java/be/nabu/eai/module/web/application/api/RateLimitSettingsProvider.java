package be.nabu.eai.module.web.application.api;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;

public interface RateLimitSettingsProvider {
	@WebResult(name = "settings")
	public RateLimitSettings settings(
			@NotNull @WebParam(name = "source") Source source, 
			@WebParam(name = "token") Token token, 
			@WebParam(name = "device") Device device, 
			@WebParam(name = "action") String action, 
			@WebParam(name = "context") String context);
}
