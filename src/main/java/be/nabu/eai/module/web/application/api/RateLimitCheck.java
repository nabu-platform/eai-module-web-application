package be.nabu.eai.module.web.application.api;

import java.util.Date;

/**
 * The amount of hits tells us how many times you hit it within the requested period
 * The oldest hit (if available) allows us to more accurately calculate when the call will work again
 */
public interface RateLimitCheck {
	public Integer getAmountOfHits();
	public Date getOldestHit();
}
