package be.nabu.eai.module.web.application;

import java.security.Principal;

import be.nabu.eai.module.web.application.api.TemporaryAuthenticator;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.principals.BasicPrincipal;
import be.nabu.libs.authentication.api.principals.DevicePrincipal;
import be.nabu.libs.authentication.api.principals.TypedPrincipal;

public class WebApplicationAuthenticator implements Authenticator {

	private Authenticator parent;
	private WebApplication application;

	public WebApplicationAuthenticator(WebApplication application, Authenticator parent) {
		this.application = application;
		this.parent = parent;
	}
	
	@Override
	public Token authenticate(String realm, Principal...credentials) {
		for (Principal credential : credentials) {
			if (credential instanceof TypedPrincipal && credential instanceof BasicPrincipal) {
				String type = ((TypedPrincipal) credential).getType();
				if (type != null && type.equals("temporary")) {
					try {
						TemporaryAuthenticator temporaryAuthenticator = application.getTemporaryAuthenticator();
						if (temporaryAuthenticator != null) {
							return temporaryAuthenticator.authenticate(realm, new TemporaryAuthenticationImpl(credential.getName(), ((BasicPrincipal) credential).getPassword()), getDevice(credentials), "authentication", null);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return parent.authenticate(realm, credentials);
	}
	
	private Device getDevice(Principal...credentials) {
		for (Principal credential : credentials) {
			if (credential instanceof DevicePrincipal) {
				return ((DevicePrincipal) credential).getDevice();
			}
		}
		return null;
	}

}
