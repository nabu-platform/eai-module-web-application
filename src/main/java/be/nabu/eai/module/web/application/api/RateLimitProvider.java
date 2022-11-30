package be.nabu.eai.module.web.application.api;

import java.util.Date;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.eai.module.web.application.rate.RateLimitResultImpl;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;

// if you want to limit on for example an API key, we currently assume you first resolve that API key to a valid token, you then get the token for rate limiting...
// in the future we might add rate limiting to non-web related things, this interface is ready for that
// for instance one might imagine a shared environment where we limit amount of service executions? or you pay per execution or...
public interface RateLimitProvider {
	@Deprecated
	@WebResult(name = "settings")
	public List<RateLimitSettings> settings(
			@NotNull @WebParam(name = "applicationId") String applicationId,
			@NotNull @WebParam(name = "source") Source source, 
			@WebParam(name = "token") Token token, 
			@WebParam(name = "device") Device device, 
			@WebParam(name = "action") String action, 
			@WebParam(name = "context") String context);
	
	@Deprecated
	@WebResult(name = "rateLimit")
	public RateLimitCheck check(
			@NotNull @WebParam(name = "applicationId") String applicationId,
			@NotNull @WebParam(name = "ruleId") String ruleId,
			@WebParam(name = "identity") String identity, 
			@WebParam(name = "context") String context, 
			@WebParam(name = "since") Date date);
	
	@Deprecated
	@WebResult(name = "rateLimit")
	public void log(
			@NotNull @WebParam(name = "applicationId") String applicationId,
			@NotNull @WebParam(name = "ruleId") String ruleId,
			@WebParam(name = "identity") String identity, 
			@WebParam(name = "context") String context, 
			@NotNull @WebParam(name = "occurred") Date date);
	
	@WebResult(name = "rateLimit")
	public default RateLimitResult rateLimit(@NotNull @WebParam(name = "applicationId") String applicationId,
			@NotNull @WebParam(name = "source") Source source, 
			@WebParam(name = "token") Token token, 
			@WebParam(name = "device") Device device, 
			@WebParam(name = "action") String action, 
			@WebParam(name = "context") String context) {
		List<RateLimitSettings> settings = settings(applicationId, source, token, device, action, context);
		Date started = new Date();
		RateLimitResultImpl result = new RateLimitResultImpl();
		// we assume default true
		result.setAllowed(true);
		if (settings != null && !settings.isEmpty()) {
			for (RateLimitSettings setting : settings) {
				// we need, at the very least, a limiting amount to be useful
				if (setting == null || setting.getAmount() == null) {
					continue;
				}
				long time = started.getTime();
				// if the interval is null, you just get a fixed amount of calls
				RateLimitCheck check = check(applicationId, setting.getRuleId(), setting.getIdentity(), setting.getContext(), setting.getInterval() == null ? null : new Date(time - setting.getInterval()));
				// if we get a result, let's validate it
				if (check != null && check.getAmountOfHits() != null) {
					// the amount of hits we get here are already done, so the current hit is +1. That's why we check >=.
					if (check.getAmountOfHits() >= setting.getAmount()) {
						Long millisecondsUntilFreeSlot = null;
						// let's see if we can determine until when
						if (check.getOldestHit() != null && setting.getInterval() != null) {
							millisecondsUntilFreeSlot = setting.getInterval() - (time - check.getOldestHit().getTime());
						}
						result.setAllowed(false);
						result.setTimeout(millisecondsUntilFreeSlot);
						result.setRuleCode(setting.getRuleCode());
						result.setRuleId(setting.getRuleId());
					}
				}
				// if we make it past the above check, log a hit
				log(applicationId, setting.getRuleId(), setting.getIdentity(), setting.getContext(), started);
			}
		}
		return result;
	}
}
