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

package be.nabu.eai.module.web.application.events;

import be.nabu.eai.repository.api.EventEnricher;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.nio.impl.RequestProcessor;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeUtils;

// for actual events: add metadata, at the very least an id!!
// clients will likely receive the same event multiple times (from different servers) if sticky sessions are not enabled
// in that case, the client needs to know if it has been handled already (it has to keep a list)

// have backend only events: no one in frontend sends them
// all events are backend only?
public class WebSessionEnricher implements EventEnricher {

	@SuppressWarnings("unchecked")
	@Override
	public Object enrich(Object object) {
		Object currentRequest = RequestProcessor.getCurrentRequest();
		if (currentRequest instanceof HTTPRequest) {
//			Pipeline pipeline = PipelineUtils.getPipeline();
//			SourceContext sourceContext = pipeline == null ? null : pipeline.getSourceContext();
//			if (sourceContext != null) {
//				String ip = sourceContext.getSocketAddress() instanceof InetSocketAddress ? ((InetSocketAddress) sourceContext.getSocketAddress()).getAddress().getHostAddress() : null;
//				if (ip != null) {
			ModifiablePart content = ((HTTPRequest) currentRequest).getContent();
			if (content != null) {
				Header header = MimeUtils.getHeader("Session-Id", content.getHeaders());
				if (header != null) {
					String value = header.getValue();
					if (!(object instanceof ComplexContent)) {
						object = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
					}
					if (object != null) {
						// if we have a field called "sessionId", we enrich it
						if (((ComplexContent) object).getType().get("sessionId") != null) {
							Object current = ((ComplexContent) object).get("sessionId");
							if (current == null) {
								((ComplexContent) object).set("sessionId", value);
							}
						}
					}
				}
			}
		}
		return null;
	}

}
