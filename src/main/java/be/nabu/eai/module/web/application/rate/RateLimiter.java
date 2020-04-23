package be.nabu.eai.module.web.application.rate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import be.nabu.eai.module.http.virtual.api.Source;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.api.RateLimitSettings;
import be.nabu.eai.module.web.application.api.RateLimitProvider;
import be.nabu.eai.module.web.application.api.RequestHandler;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.metrics.core.api.HistorySink;
import be.nabu.libs.metrics.core.api.SinkProvider;
import be.nabu.libs.metrics.core.api.SinkSnapshot;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class RateLimiter implements RequestHandler {
	private Map<String, RateLimitSettings> settings = new HashMap<String, RateLimitSettings>();
	private Map<String, RateLimitProvider> providers = new HashMap<String, RateLimitProvider>();
	private DefinedService service;
	private SinkProvider provider;
	private Repository repository;
	
	public RateLimiter(Repository repository, DefinedService service, SinkProvider provider) {
		this.repository = repository;
		this.service = service;
		this.provider = provider;
	}
	
	@Override
	public HTTPResponse handle(WebApplication application, HTTPRequest request, Source source, Token token, Device device, String action, String context) {
//		String identityKey = application.getId() + "@" + source.getRemoteHost();
//		if (token != null) {
//			identityKey += ":" + token.getRealm() + ":" + token.getName();
//		}
//		else {
//			identityKey += "::";
//		}
//		// you can alter the device key easily at the clients end but all that forces is an additional lookup, not actual rate limiting circumvention
//		if (device != null) {
//			identityKey += "$" + device.getDeviceId();
//		}
//		else {
//			identityKey += "$";
//		}
//		if (action != null) {
//			identityKey += ">" + action;
//		}
//		else {
//			identityKey += ">";
//		}
//		if (context != null) {
//			identityKey += "@" + context;
//		}
//		else {
//			identityKey += "@";
//		}
//		if (!settings.containsKey(identityKey)) {
//			synchronized(settings) {
//				if (!settings.containsKey(identityKey)) {
//					settings.put(identityKey, getProvider(application).settings(source, token, device, action, context));
//				}
//			}
//		}
//		RateLimitSettings rateLimitSettings = settings.get(identityKey);
//		// if unlimited, we don't care
//		if (rateLimitSettings == null || rateLimitSettings.getAmount() == null || rateLimitSettings.getAmount() == 0) {
//			return null;
//		}
//		HistorySink sink = (HistorySink) provider.getSink(rateLimitSettings.getIdentity() == null ? identityKey : rateLimitSettings.getIdentity(), rateLimitSettings.getContext() == null ? action : rateLimitSettings.getContext());
//		long time = new Date().getTime();
//		SinkSnapshot snapshotBetween = sink.getSnapshotBetween(time - rateLimitSettings.getInterval(), time);
//		if (snapshotBetween.getValues().size() >= rateLimitSettings.getAmount()) {
//			PlainMimeEmptyPart content = new PlainMimeEmptyPart(null, 
//				new MimeHeader("Content-Length", "0"));
//			if (!snapshotBetween.getValues().isEmpty()) {
//				long millisecondsUntilFreeSlot = rateLimitSettings.getInterval() - (time - snapshotBetween.getValues().get(0).getTimestamp());
//				content.setHeader(new MimeHeader("Retry-After", "" + (millisecondsUntilFreeSlot / 1000)));
//			}
//			return new DefaultHTTPResponse(429, HTTPCodes.getMessage(429), content);
//		}
//		else {
//			sink.push(time, 1);
//		}
		return null;
	}

	private RateLimitProvider getProvider(WebApplication application) {
		if (!providers.containsKey(application.getId())) {
			synchronized(this) {
				if (!providers.containsKey(application.getId())) {
					providers.put(application.getId(), POJOUtils.newProxy(RateLimitProvider.class, application.wrap(service, WebApplication.getMethod(RateLimitProvider.class, "settings")), repository, SystemPrincipal.ROOT));
				}
			}
		}
		return providers.get(application.getId());
	}
}
