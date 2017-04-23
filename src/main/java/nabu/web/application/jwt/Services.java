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

import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.libs.http.jwt.JWTBody;
import be.nabu.libs.http.jwt.JWTUtils;
import be.nabu.libs.http.jwt.enums.JWTAlgorithm;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.types.api.KeyValuePair;

@WebService
public class Services {
	
	private ExecutionContext context;
	
	public JWTBody unmarshal(
			@WebParam(name = "keystore") String keystore, 
			@WebParam(name = "keyAlias") String keyAlias,
			@WebParam(name = "content") String content) throws KeyStoreException, IOException, ParseException {
		
		Key key = null;
		
		if (keystore != null && keyAlias != null) {
			KeyStoreArtifact keystoreArtifact = context.getServiceContext().getResolver(KeyStoreArtifact.class).resolve(keystore);
			if (keystoreArtifact == null) {
				throw new IllegalArgumentException("No keystore found by the name: " + keystore);
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
		}
		
		return JWTUtils.decode(key, content);
	}
	
	public String marshal(
			@WebParam(name = "keystore") String keystore, 
			@WebParam(name = "keyAlias") String keyAlias, 
			@WebParam(name = "issuedAt") Date issuedAt, 
			@WebParam(name = "notBefore") Date notBefore, 
			@WebParam(name = "validUntil") Date validUntil, 
			@WebParam(name = "issuer") String issuer, 
			@WebParam(name = "subject") String subject,
			@WebParam(name = "audience") String audience,
			@WebParam(name = "realm") String realm,
			@WebParam(name = "properties") List<KeyValuePair> pairs,
			@WebParam(name = "algorithm") JWTAlgorithm algorithm) throws KeyStoreException, IOException {
		
		JWTBody body = new JWTBody();
		
		body.setRlm(realm);
		// one hour by default
		if (validUntil == null) {
			validUntil = new Date(new Date().getTime() + (1000l*60*60));
		}
		body.setExp(validUntil.getTime() / 1000);
		
		if (issuedAt == null) {
			issuedAt = new Date();
		}
		body.setIat(issuedAt.getTime() / 1000);
		
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
		
		body.setValues(pairs);
		
		KeyStoreArtifact keystoreArtifact = context.getServiceContext().getResolver(KeyStoreArtifact.class).resolve(keystore);
		if (keystoreArtifact == null) {
			throw new IllegalArgumentException("No keystore found by the name: " + keystore);
		}
		Key key;
		try {
			key = keystoreArtifact.getKeyStore().getPrivateKey(keyAlias);
		}
		catch (Exception e) {
			key = keystoreArtifact.getKeyStore().getSecretKey(keyAlias);
		}
		
		if (algorithm == null) {
			algorithm = key instanceof SecretKey ? JWTAlgorithm.HS512 : JWTAlgorithm.RS512;
		}
		
		return JWTUtils.encode(key, body, algorithm);
	}
}
