package be.nabu.eai.module.web.application.rate;

import be.nabu.eai.module.web.application.api.RateLimitResult;

public class RateLimitResultImpl implements RateLimitResult {

	private boolean allowed;
	private Long timeout;
	private String ruleCode, ruleId;
	private Integer amountOfHits, maxHits;
	
	@Override
	public boolean isAllowed() {
		return allowed;
	}
	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}
	@Override
	public Long getTimeout() {
		return timeout;
	}
	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}
	@Override	
	public String getRuleCode() {
		return ruleCode;
	}
	public void setRuleCode(String ruleCode) {
		this.ruleCode = ruleCode;
	}
	@Override
	public String getRuleId() {
		return ruleId;
	}
	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}
	@Override
	public Integer getAmountOfHits() {
		return amountOfHits;
	}
	public void setAmountOfHits(Integer amountOfHits) {
		this.amountOfHits = amountOfHits;
	}
	@Override
	public Integer getMaxHits() {
		return maxHits;
	}
	public void setMaxHits(Integer maxHits) {
		this.maxHits = maxHits;
	}
	
}
