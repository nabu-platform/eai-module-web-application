package be.nabu.eai.module.web.application.api;

import java.net.URI;

import javax.jws.WebParam;
import javax.jws.WebResult;

public interface CORSHandler {
	// the origin is the value of the origin header, the uri is the target uri being accessed
	@WebResult(name = "cors")
	public CORSResult check(@WebParam(name = "applicationId") String applicationId, @WebParam(name = "origin") String origin, @WebParam(name = "uri") URI uri);
}
