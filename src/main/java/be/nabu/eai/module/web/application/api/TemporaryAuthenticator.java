package be.nabu.eai.module.web.application.api;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;

public interface TemporaryAuthenticator {
	@WebResult(name = "token")
	public Token authenticate(@NotNull @WebParam(name = "realm") String realm, @NotNull @WebParam(name = "alias") String alias, @NotNull @WebParam(name = "secret") String secret, @WebParam(name = "device") Device device);
}
