package be.nabu.eai.module.web.application;

import be.nabu.eai.module.web.application.api.TemporaryAuthentication;

public class TemporaryAuthenticationImpl implements TemporaryAuthentication {

	private String id, secret;
	
	public TemporaryAuthenticationImpl() {
		// auto construct
	}
	public TemporaryAuthenticationImpl(String id, String secret) {
		this.id = id;
		this.secret = secret;
	}
	
	@Override
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	
}
