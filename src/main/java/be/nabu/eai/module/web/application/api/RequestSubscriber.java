package be.nabu.eai.module.web.application.api;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;

public interface RequestSubscriber {
	@WebResult(name = "response")
	public HTTPResponse handle(@WebParam(name = "webApplicationId") @NotNull String webApplicationId, @WebParam(name = "realm") @NotNull String realm, @NotNull @WebParam(name = "source") Source source, @NotNull @WebParam(name = "request") HTTPRequest request);
}
