package be.nabu.eai.module.web.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import be.nabu.eai.developer.api.InterfaceLister;
import be.nabu.eai.developer.util.InterfaceDescriptionImpl;

public class WebApplicationInterfaceLister implements InterfaceLister {

	private static Collection<InterfaceDescription> descriptions = null;
	
	@Override
	public Collection<InterfaceDescription> getInterfaces() {
		if (descriptions == null) {
			synchronized(WebApplicationInterfaceLister.class) {
				if (descriptions == null) {
					List<InterfaceDescription> descriptions = new ArrayList<InterfaceDescription>();
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Password Authenticator", "be.nabu.eai.authentication.api.PasswordAuthenticator.authenticate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Secret Authenticator", "be.nabu.eai.authentication.api.SecretAuthenticator.authenticate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Role Handler", "be.nabu.libs.authentication.api.RoleHandler.hasRole"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Permission Handler", "be.nabu.libs.authentication.api.PermissionHandler.hasPermission"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Potential Permission Handler", "be.nabu.libs.authentication.api.PotentialPermissionHandler.hasPotentialPermission"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Device Validator", "be.nabu.libs.authentication.api.DeviceValidator.isAllowed"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Translator", "be.nabu.eai.repository.api.Translator.translate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "User Language Provider", "be.nabu.eai.repository.api.UserLanguageProvider.getLanguage"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Request Language Provider", "be.nabu.eai.module.web.application.api.RequestLanguageProvider.getLanguage"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Language Provider", "be.nabu.eai.repository.api.LanguageProvider.getSupportedLanguages"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Token Validator", "be.nabu.libs.authentication.api.TokenValidator.isValid"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Request Subscriber", "be.nabu.eai.module.web.application.api.RequestSubscriber.handle"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Rate Limit Settings", "be.nabu.eai.module.web.application.api.RateLimitProvider.settings"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Rate Limit Checker", "be.nabu.eai.module.web.application.api.RateLimitProvider.check"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Rate Limit Logger", "be.nabu.eai.module.web.application.api.RateLimitProvider.log"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Bearer Authenticator", "be.nabu.eai.module.web.application.api.BearerAuthenticator.authenticate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Temporary Authenticator", "be.nabu.eai.module.web.application.api.TemporaryAuthenticator.authenticate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Temporary Authentication Generator", "be.nabu.eai.module.web.application.api.TemporaryAuthenticationGenerator.generate"));
					WebApplicationInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}

}
