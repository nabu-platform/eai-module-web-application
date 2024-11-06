/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.web.application;

import java.util.Date;

import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.glue.impl.UserMethods;
import be.nabu.libs.metrics.core.api.SinkEvent;

/**
 * When this listener is triggered, it will blacklist the ip
 * It presumes that filters will block the triggering of this listener until such a time that an ip has to be blocked
 */
public class FailedLoginListener implements EventHandler<SinkEvent, Void> {

	private WebApplication artifact;
	private long duration;

	public FailedLoginListener(WebApplication artifact, long duration) {
		this.artifact = artifact;
		this.duration = duration;
	}
	
	@Override
	public Void handle(SinkEvent event) {
		// also skip the ":" at the end
		String ip = event.getCategory().substring(UserMethods.METRICS_LOGIN_FAILED.length() + 1);
		artifact.getListener().blacklistLogin(ip, new Date(new Date().getTime() + duration));
		return null;
	}

}
