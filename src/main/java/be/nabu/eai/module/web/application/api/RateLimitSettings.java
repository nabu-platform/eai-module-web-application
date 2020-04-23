package be.nabu.eai.module.web.application.api;

import javax.validation.constraints.NotNull;

public interface RateLimitSettings {
	/**
	 * The identity we limit on, this can be the device, the user account, the ip,...
	 * There can be multiple identities, for example suppose you want to limit bad logins
	 * You can limit one IP from making too many attempts, but you can also block multiple IPs from making too many attempts on a single account
	 */
	public String getIdentity();
	/**
	 * The context we limit on (could for example be the action or the original context, or a combination of both or neither
	 */
	public String getContext();
	/**
	 * The amount of times you can execute it
	 */
	@NotNull
	public Integer getAmount();
	/**
	 * The interval for which the amounts are valid
	 * No interval means the amount is forever
	 */
	public Long getInterval();
	/**
	 * Some way to link back to the original rule id
	 */
	@NotNull
	public String getRuleId();
	/**
	 * A more readable code to clarify what went wrong
	 */
	public String getRuleCode();
}
