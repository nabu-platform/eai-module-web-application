package be.nabu.eai.module.web.application;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import be.nabu.eai.authentication.api.PasswordAuthenticator;
import be.nabu.eai.authentication.api.SecretAuthenticator;
import be.nabu.eai.authentication.api.TypedAuthenticator;
import be.nabu.eai.module.web.application.api.TemporaryAuthenticator;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.principals.BasicPrincipal;
import be.nabu.libs.authentication.api.principals.DevicePrincipal;
import be.nabu.libs.authentication.api.principals.SharedSecretPrincipal;
import be.nabu.libs.authentication.api.principals.TypedPrincipal;
import be.nabu.libs.services.ServiceRuntime;

public class WebApplicationAuthenticator implements Authenticator {

	private final class SharedSecretPrincipalImplementation implements SharedSecretPrincipal {
		private final BasicPrincipal credential;
		private static final long serialVersionUID = 1L;

		private SharedSecretPrincipalImplementation(BasicPrincipal credential) {
			this.credential = credential;
		}

		@Override
		public String getName() {
			return credential.getName();
		}

		@Override
		public String getSecret() {
			return credential.getPassword();
		}
	}

	private Authenticator parent;
	private WebApplication application;

	public WebApplicationAuthenticator(WebApplication application, Authenticator parent) {
		this.application = application;
		this.parent = parent;
	}
	
	@Override
	public Token authenticate(String realm, Principal...credentials) {
		Map<String, Object> originalGlobalContext = ServiceRuntime.getGlobalContext();
		try {
			Device device = getDevice(credentials);
			
			// set it globally
			// this is actually not ok because we can call authenticate from another service!
			// this should be refactored to only be called if it is the root service?
			ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
			ServiceRuntime.getGlobalContext().put("service.context", application.getId());
			ServiceRuntime.getGlobalContext().put("webApplicationId", application.getId());
			ServiceRuntime.getGlobalContext().put("service.source", "web");
			for (Principal credential : credentials) {
				if (credential instanceof TypedPrincipal) {
					TypedAuthenticator typedAuthenticator = application.getTypedAuthenticator();
					if (typedAuthenticator != null) {
						Token token = typedAuthenticator.authenticate(realm, (TypedPrincipal) credential, device);
						if (token != null) {
							return token;
						}
					}
				}
				if (credential instanceof TypedPrincipal && credential instanceof BasicPrincipal) {
					String type = ((TypedPrincipal) credential).getType();
					// password is the default
					if (type == null || type.equals("password")) {
						PasswordAuthenticator passwordAuthenticator = application.getPasswordAuthenticator();
						if (passwordAuthenticator == null) {
							throw new IllegalStateException("No password authenticator has been configured for application: " + application.getId());
						}
						return passwordAuthenticator.authenticate(realm, (BasicPrincipal) credential, device);
					}
					else if (type.equals("secret")) {
						SecretAuthenticator secretAuthenticator = application.getSecretAuthenticator();
						if (secretAuthenticator == null) {
							throw new IllegalStateException("No secret authenticator has been configured for application: " + application.getId());
						}
						return secretAuthenticator.authenticate(realm, credential instanceof SharedSecretPrincipal ? (SharedSecretPrincipal) credential : new SharedSecretPrincipalImplementation((BasicPrincipal) credential), device);
					}
					else if (type.equals("temporary")) {
						try {
							TemporaryAuthenticator temporaryAuthenticator = application.getTemporaryAuthenticator();
							if (temporaryAuthenticator != null) {
								return temporaryAuthenticator.authenticate(realm, new TemporaryAuthenticationImpl(credential.getName(), ((BasicPrincipal) credential).getPassword()), device, "authentication", null);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				else if (credential instanceof SharedSecretPrincipal) {
					SecretAuthenticator secretAuthenticator = application.getSecretAuthenticator();
					if (secretAuthenticator == null) {
						throw new IllegalStateException("No secret authenticator has been configured for application: " + application.getId());
					}
					return secretAuthenticator.authenticate(realm, (SharedSecretPrincipal) credential, device);
				}
			}
			return parent == null ? null : parent.authenticate(realm, credentials);
		}
		finally {
			// unset it
			ServiceRuntime.setGlobalContext(originalGlobalContext);	
		}
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
