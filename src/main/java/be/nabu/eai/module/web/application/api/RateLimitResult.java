package be.nabu.eai.module.web.application.api;

public interface RateLimitResult {
	// whether or not it is allowed to pass
	public boolean isAllowed();
	// the timeout (if available) after which we can try again
	public Long getTimeout();
	
	// metadata for logging
	public String getRuleId();
	public String getRuleCode();
	
	public Integer getAmountOfHits();
	public Integer getMaxHits();
}
