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

package nabu.web.application.jwt;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStoreException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;
import javax.jws.WebParam;
import javax.jws.WebService;

import be.nabu.eai.api.Comment;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.libs.http.jwt.JWTBody;
import be.nabu.libs.http.jwt.JWTUtils;
import be.nabu.libs.http.jwt.enums.JWTAlgorithm;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.types.api.KeyValuePair;

@WebService
public class Services {
	
	private ExecutionContext context;
	
	@Comment(title = "Either you provide a valid keystore/keyalias combination or you provide a web application which has these things configured in it")
	public JWTBody unmarshal(
			@WebParam(name = "webApplicationId") String webApplicationId,
			@WebParam(name = "keystore") String keystore, 
			@WebParam(name = "keyAlias") String keyAlias,
			@WebParam(name = "content") String content) throws KeyStoreException, IOException, ParseException {
		
		Key key = null;
		
		// if you provide input for actual validation, we will validate or throw an exception
		// if you explicitly do not provide these inputs, we will simply parse without validating
		if (keystore != null || keyAlias != null || webApplicationId != null) {
			KeyStoreArtifact keystoreArtifact = keystore == null ? null : context.getServiceContext().getResolver(KeyStoreArtifact.class).resolve(keystore);
			WebApplication resolve = null;
			// if no keystore was found, check the web application (if any)
			// if you fill in both the keystore input and the web application input, you are already doing something "weird" and hopefully know the sequence of events that takes place here
			if (webApplicationId != null && keystoreArtifact == null) {
				resolve = context.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);
				keystoreArtifact = resolve.getConfig().getJwtKeyStore();
			}
			
			if (keystoreArtifact == null) {
				throw new IllegalArgumentException("No keystore found (keystore: " + keystore + ", web application: " + webApplicationId + ")");
			}
			
			if (keyAlias == null && webApplicationId != null) {
				if (resolve == null) {
					resolve = context.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);	
				}
				keyAlias = resolve.getConfig().getJwtKeyAlias();
			}
			if (keyAlias == null) {
				throw new IllegalArgumentException("Can not find correct key alias");
			}
			try {
				key = keystoreArtifact.getKeyStore().getCertificate(keyAlias).getPublicKey();
			}
			catch (Exception e) {
				try {
					key = keystoreArtifact.getKeyStore().getChain(keyAlias)[0].getPublicKey();
				}
				catch (Exception f) {
					key = keystoreArtifact.getKeyStore().getSecretKey(keyAlias);
				}
			}
			if (key == null) {
				throw new IllegalArgumentException("Can not resolve key '" + keyAlias + "' in keystore '" + keystore + "'");
			}
			
		}
		
		return JWTUtils.decode(key, content);
	}
	
	@Comment(title = "Either you provide a valid keystore/keyalias combination or you provide a web application which has these things configured in it")
	public String marshal(
			@WebParam(name = "webApplicationId") String webApplicationId,
			@WebParam(name = "keystore") String keystore, 
			@WebParam(name = "keyAlias") String keyAlias, 
			@WebParam(name = "issuedAt") Date issuedAt, 
			@WebParam(name = "notBefore") Date notBefore, 
			@WebParam(name = "validUntil") Date validUntil, 
			@WebParam(name = "issuer") String issuer, 
			@WebParam(name = "subject") String subject,
			@WebParam(name = "audience") String audience,
			@WebParam(name = "realm") String realm,
			@WebParam(name = "jwtId") String jwtId,
			@WebParam(name = "properties") List<KeyValuePair> pairs,
			@WebParam(name = "algorithm") JWTAlgorithm algorithm) throws KeyStoreException, IOException {
		
		JWTBody body = new JWTBody();
		
		body.setRlm(realm);

		if (validUntil != null) {
			body.setExp(validUntil.getTime() / 1000);
		}
		if (issuedAt != null) {
			body.setIat(issuedAt.getTime() / 1000);
		}
		if (notBefore != null) {
			body.setNbf(notBefore.getTime() / 1000);
		}
		if (issuer != null) {
			body.setIss(issuer);
		}
		if (subject != null) {
			body.setSub(subject);
		}
		if (audience != null) {
			body.setAud(audience);
		}
		if (jwtId != null) {
			body.setJti(jwtId);
		}
		if (pairs != null && !pairs.isEmpty()) {
			body.setValues(pairs);
		}
		
		KeyStoreArtifact keystoreArtifact = keystore == null ? null : context.getServiceContext().getResolver(KeyStoreArtifact.class).resolve(keystore);
		WebApplication resolve = null;
		// if no keystore was found, check the web application (if any)
		// if you fill in both the keystore input and the web application input, you are already doing something "weird" and hopefully know the sequence of events that takes place here
		if (webApplicationId != null && keystoreArtifact == null) {
			resolve = context.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);
			keystoreArtifact = resolve.getConfig().getJwtKeyStore();
		}
		
		if (keystoreArtifact == null) {
			throw new IllegalArgumentException("No keystore found (keystore: " + keystore + ", web application: " + webApplicationId + ")");
		}
		if (keyAlias == null && webApplicationId != null) {
			if (resolve == null) {
				resolve = context.getServiceContext().getResolver(WebApplication.class).resolve(webApplicationId);	
			}
			keyAlias = resolve.getConfig().getJwtKeyAlias();
		}
		if (keyAlias == null) {
			throw new IllegalArgumentException("Can not find correct key alias");
		}
		
		Key key;
		try {
			key = keystoreArtifact.getKeyStore().getPrivateKey(keyAlias);
		}
		catch (Exception e) {
			key = keystoreArtifact.getKeyStore().getSecretKey(keyAlias);
		}
		if (key == null) {
			throw new IllegalArgumentException("Can not resolve key '" + keyAlias + "' in keystore '" + keystore + "'");
		}
		
		// instead of opting for the most secure, we balance overall jwt token size with security
		// if you want the more secure algorithms, set it explicitly
		if (algorithm == null) {
			algorithm = key instanceof SecretKey ? JWTAlgorithm.HS256 : JWTAlgorithm.RS256;
		}
		return JWTUtils.encode(key, body, algorithm);
	}
}
