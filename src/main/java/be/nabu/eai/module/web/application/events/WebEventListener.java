package be.nabu.eai.module.web.application.events;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.server.Server;
import be.nabu.eai.server.api.ServerListener;
import be.nabu.libs.http.api.server.HTTPServer;

public class WebEventListener implements ServerListener {

	@Override
	public void listen(Server server, HTTPServer httpServer) {
		EAIResourceRepository.getInstance().addEventEnricher("web-event-session", new WebSessionEnricher());
	}

}
