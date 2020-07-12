package be.nabu.eai.module.web.application.api;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;

public interface TemporaryAuthenticator {
	
	public static final String EXECUTION = "execution";
	public static final String AUTHENTICATION = "authentication";
	
	@WebResult(name = "token")
	public Token authenticate(@NotNull @WebParam(name = "realm") String realm, 
			@WebParam(name = "authentication") @NotNull TemporaryAuthentication authentication, 
			@WebParam(name = "device") Device device, 
			@NotNull @WebParam(name = "type") String type, 
			@WebParam(name = "correlationId") String correlationId);
}
