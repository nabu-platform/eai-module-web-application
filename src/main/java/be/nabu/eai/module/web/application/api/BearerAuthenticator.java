package be.nabu.eai.module.web.application.api;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;

public interface BearerAuthenticator {
	@WebResult(name = "token")
	public Token authenticate(@NotNull @WebParam(name = "webApplicationId") String webApplicationId, @NotNull @WebParam(name = "realm") String realm, @NotNull @WebParam(name = "bearer") String bearer, @WebParam(name = "device") Device device);
}
