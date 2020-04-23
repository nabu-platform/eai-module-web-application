package be.nabu.eai.module.web.application;

import be.nabu.eai.module.web.application.api.RateLimit;

public class RateLimitImpl implements RateLimit {
	private String action, context;
	
	public RateLimitImpl() {
		// auto
	}
	
	public RateLimitImpl(String action, String context) {
		this.action = action;
		this.context = context;
	}

	@Override
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}

	@Override
	public String getContext() {
		return context;
	}
	public void setContext(String context) {
		this.context = context;
	}
}
