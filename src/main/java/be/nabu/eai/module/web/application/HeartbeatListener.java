package be.nabu.eai.module.web.application;

import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;

import org.slf4j.LoggerFactory;

import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.core.ServerHeader;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

public class HeartbeatListener implements EventHandler<HTTPRequest, HTTPResponse> {
	
	@ComplexTypeDescriptor(name = "heartbeat")
	public static class Heartbeat {
		private Date date;
		private Double absoluteLoad, relativeLoad;
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		public Double getAbsoluteLoad() {
			return absoluteLoad;
		}
		public void setAbsoluteLoad(Double absoluteLoad) {
			this.absoluteLoad = absoluteLoad;
		}
		public Double getRelativeLoad() {
			return relativeLoad;
		}
		public void setRelativeLoad(Double relativeLoad) {
			this.relativeLoad = relativeLoad;
		}
	}
	
	private static OperatingSystemMXBean operatingSystemMXBean;
	private WebApplication application;
	
	static {
		try {
			 operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
		}
		catch (Exception e) {
			LoggerFactory.getLogger(HeartbeatListener.class).warn("Heartbeat does not support load reporting: " + e.getMessage());
		}
	}
	
	public HeartbeatListener(WebApplication application) {
		this.application = application;
	}

	@Override
	public HTTPResponse handle(HTTPRequest request) {
		try {
			URI uri = HTTPUtils.getURI(request, false);
			String path = URIUtils.normalize(uri.getPath());
			String heartbeatPath = application.getServerPath();
			if (!heartbeatPath.endsWith("/")) {
				heartbeatPath += "/";
			}
			heartbeatPath += "heartbeat";
			if (heartbeatPath.equals(path)) {
				Heartbeat heartbeat = new Heartbeat();
				heartbeat.setDate(new Date());
				// if we are proxied, include load information IF the request does not include a source IP, so it is coming from the proxy itself, not the outside world
				if (application.getConfig().getVirtualHost().getConfig().getServer().getConfig().isProxied()) {
					Header remoteAddress = MimeUtils.getHeader(ServerHeader.REMOTE_ADDRESS.getName(), request.getContent().getHeaders());
					if (remoteAddress == null) {
						heartbeat.setAbsoluteLoad(operatingSystemMXBean.getSystemLoadAverage());
						heartbeat.setRelativeLoad(operatingSystemMXBean.getSystemLoadAverage() / operatingSystemMXBean.getAvailableProcessors());
					}
				}
				JSONBinding binding = new JSONBinding((ComplexType) BeanResolver.getInstance().resolve(Heartbeat.class), Charset.forName("UTF-8"));
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				binding.marshal(output, new BeanInstance<Heartbeat>(heartbeat));
				byte[] content = output.toByteArray();
				return new DefaultHTTPResponse(request, 200, HTTPCodes.getMessage(200), new PlainMimeContentPart(null, IOUtils.wrap(content, true),
					new MimeHeader("Content-Length", "" + content.length),
					new MimeHeader("Content-Type", "application/json")
				));
			}
			return null;
		}
		catch (Exception e) {
			HTTPException httpException = new HTTPException(500, e);
			httpException.getContext().add(application.getId());
			throw httpException;
		}
	}
}
