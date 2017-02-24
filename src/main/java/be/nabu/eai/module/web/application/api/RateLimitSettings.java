package be.nabu.eai.module.web.application.api;

import javax.validation.constraints.NotNull;

public interface RateLimitSettings {
	/**
	 * The identity we limit on
	 */
	public String getIdentity();
	/**
	 * The context we limit on
	 */
	public String getContext();
	/**
	 * The amount of times you can execute it
	 */
	@NotNull
	public Integer getAmount();
	/**
	 * The interval for which the amounts are valid
	 */
	@NotNull
	public Long getInterval();
}
