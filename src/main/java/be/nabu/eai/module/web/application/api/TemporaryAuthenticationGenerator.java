package be.nabu.eai.module.web.application.api;

import java.util.Date;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

public interface TemporaryAuthenticationGenerator {
	public TemporaryAuthentication generate(@NotNull @WebParam(name = "webApplicationId") String webApplicationId, @NotNull @WebParam(name = "realm") String realm, @NotNull @WebParam(name = "alias") String alias, @WebParam(name = "maxUses") Integer maxUses, @WebParam(name = "until") Date until, @WebParam(name = "type") String type, @WebParam(name = "correlationId") String correlationId);
}
