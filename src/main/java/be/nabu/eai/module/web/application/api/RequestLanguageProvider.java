package be.nabu.eai.module.web.application.api;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.http.api.HTTPRequest;

public interface RequestLanguageProvider {
	@WebResult(name = "language")
	public String getLanguage(@WebParam(name = "request") HTTPRequest request);
}
