package be.nabu.eai.module.web.application.api;

import java.util.Map;

import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.types.api.ComplexContent;

public interface ExecutableRESTFragment extends RESTFragment {
	// the request is there to extract additional metadata, not to further parser query/header/... parameters
	public ComplexContent execute(HTTPRequest request, Map<String, Object> parameters, ComplexContent body);
}
