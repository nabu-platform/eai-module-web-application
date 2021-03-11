package be.nabu.eai.module.web.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.module.web.application.api.RESTFragment;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.types.structure.StructureUtils;

public class ServiceRESTFragment implements RESTFragment {
	private final DefinedService service;
	private final WebApplication application;
	private ComplexType input, output;
	private String method;
	private List<Element<?>> query;

	ServiceRESTFragment(DefinedService service, WebApplication application) {
		this.service = service;
		this.application = application;
	}

	@Override
	public String getId() {
		return service.getId();
	}

	@Override
	public List<Element<?>> getQueryParameters() {
		if (getMethod().equals("GET")) {
			if (query == null) {
				synchronized(this) {
					if (query == null) {
						List<Element<?>> parameters = new ArrayList<Element<?>>();
						RESTServiceListener.buildQueryParameters(getScopedInput(), new ArrayList<ComplexType>(), null, parameters);
						query = parameters;
					}
				}
			}
		}
		return query;
	}

	@Override
	public List<String> getConsumes() {
		return getMethod().equals("GET") ? new ArrayList<String>() : Arrays.asList("application/json", "application/xml");
	}

	@Override
	public List<String> getProduces() {
		return Arrays.asList("application/json", "application/xml");
	}

	// never path parameters atm?
	@Override
	public List<Element<?>> getPathParameters() {
		return new ArrayList<Element<?>>();
	}

	@Override
	public String getPath() {
		return application.getServicePath() + "/" + service.getId().replace(".", "/");
	}

	@Override
	public ComplexType getOutput() {
		if (output == null) {
			synchronized(this) {
				if (output == null) {
					ComplexType outputDefinition = service.getServiceInterface().getOutputDefinition();
					Structure structure = new Structure();
					structure.setName("output");
					output = outputDefinition == null ? structure : StructureUtils.scope(outputDefinition);
				}
			}
		}
		return output;
	}

	@Override
	public ComplexType getInput() {
		return getMethod().equals("GET") ? null : getScopedInput();
	}

	public ComplexType getScopedInput() {
		if (input == null) {
			synchronized(this) {
				if (input == null) {
					ComplexType inputDefinition = service.getServiceInterface().getInputDefinition();
					Structure structure = new Structure();
					structure.setName("input");
					input = inputDefinition == null ? structure : StructureUtils.scope(inputDefinition);
				}
			}
		}
		return input;
	}

	@Override
	public String getMethod() {
		if (method == null) {
			synchronized (this) {
				if (method == null) {
					method = RESTServiceListener.isGetCompatible(getScopedInput(), new ArrayList<ComplexType>()) ? "GET" : "POST";
				}
			}
		}
		return method;
	}

	// no header params atm
	@Override
	public List<Element<?>> getHeaderParameters() {
		return new ArrayList<Element<?>>();
	}

	@Override
	public String getPermissionAction() {
		return service.getId();
	}

	@Override
	public String getRateLimitAction() {
		return service.getId();
	}
	
	// by default in sync with the api component we generated
	// currently we can't change the tags yet for services
	public List<String> getTags() {
		return Arrays.asList("Api");
	}
	
}