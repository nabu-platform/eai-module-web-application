package be.nabu.eai.module.web.application.api;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

public interface TemporaryAuthenticationRevoker {
	public void revoke(
		// the web application this is for
		@NotNull @WebParam(name = "webApplicationId") String webApplicationId,
		@NotNull @WebParam(name = "tokenId") String tokenId);
}
