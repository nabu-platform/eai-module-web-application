package be.nabu.eai.module.web.application.api;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.http.api.HTTPRequest;

public interface ArbitraryAuthenticator {
	@WebResult(name = "token")
	public Token authenticate(@NotNull @WebParam(name = "realm") String realm, @NotNull @WebParam(name = "request") HTTPRequest request, @WebParam(name = "device") Device device);
}