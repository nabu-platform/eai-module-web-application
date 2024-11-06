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
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Secret Generator", "be.nabu.libs.authentication.api.SecretGenerator.generate"));
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
//					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Rate Limit Settings", "be.nabu.eai.module.web.application.api.RateLimitProvider.settings"));
//					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Rate Limit Checker", "be.nabu.eai.module.web.application.api.RateLimitProvider.check"));
//					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Rate Limit Logger", "be.nabu.eai.module.web.application.api.RateLimitProvider.log"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Rate Limiter", "be.nabu.eai.module.web.application.api.RateLimitProvider.rateLimit"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Bearer Authenticator", "be.nabu.eai.module.web.application.api.BearerAuthenticator.authenticate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Arbitrary Authenticator", "be.nabu.eai.module.web.application.api.ArbitraryAuthenticator.authenticate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Temporary Authenticator", "be.nabu.eai.module.web.application.api.TemporaryAuthenticator.authenticate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Temporary Authentication Generator", "be.nabu.eai.module.web.application.api.TemporaryAuthenticationGenerator.generate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Temporary Authentication Revoker", "be.nabu.eai.module.web.application.api.TemporaryAuthenticationRevoker.revoke"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "Typed Authenticator", "be.nabu.eai.authentication.api.TypedAuthenticator.authenticate"));
					descriptions.add(new InterfaceDescriptionImpl("Web Application", "CORS Checker", "be.nabu.eai.module.web.application.api.CORSHandler.check"));
					WebApplicationInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}

}
