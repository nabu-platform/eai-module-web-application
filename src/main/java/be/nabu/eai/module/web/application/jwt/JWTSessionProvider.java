package be.nabu.eai.module.web.application.jwt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.Principal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.authentication.TokenSerializerFactory;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.api.TokenSerializer;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.api.server.SessionProvider;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;

@Deprecated
public class JWTSessionProvider implements SessionProvider {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Map<String, JWTSession> sessions = new HashMap<String, JWTSession>();
	
	private SecretKey key;

	private long sessionTimeout;

	private String tokenKey;

	public JWTSessionProvider(String tokenKey, SecretKey key, long sessionTimeout) {
		this.tokenKey = tokenKey;
		this.key = key;
		this.sessionTimeout = sessionTimeout;
	}
	
	@Override
	public Session getSession(String sessionId) {
		JWTSession session = sessions.get(sessionId);
		if (session == null && sessionId.matches(".+\\..+\\..+")) {
			try {
				session = new JWTSession(sessionId);
				if (session != null) {
					synchronized(sessions) {
						sessions.put(session.getId(), session);
					}
				}
			}
			catch (ParseException e) {
				logger.warn("Invalid JWT session: " + sessionId, e);
			}
		}
		if (session != null) {
			Date timeout = session.get(tokenKey) instanceof JWTToken ? ((JWTToken) session.get(tokenKey)).getValidUntil() : new Date(session.getLastAccessed().getTime() + sessionTimeout);
			// if the session is timed out, remove it
			if (timeout.before(new Date())) {
				session.destroy();
				session = null;
			}
			else {
				session.setLastAccessed(new Date());
			}
		}
		return session;
	}

	@Override
	public Session newSession() {
		JWTSession session = new JWTSession();
		synchronized(sessions) {
			sessions.put(session.getId(), session);
		}
		return session;
	}

	public class JWTSession implements Session {
	
		private Date lastAccessed = new Date();
		private Map<String, Object> context = new HashMap<String, Object>();
		
		private String id, originalId;
		
		// generate with an existing jwt token
		public JWTSession(String token) throws ParseException {
			JWTBody jwt = TypeUtils.getAsBean(JWTUtils.decodeJWTSecret((ComplexType) BeanResolver.getInstance().resolve(JWTBody.class), token, key), JWTBody.class);
			this.id = token;
			if (jwt.getTkn() != null && jwt.getTkt() != null) {
				TokenSerializer<Token> serializer = TokenSerializerFactory.getInstance().getSerializer(jwt.getTkt());
				if (serializer == null) {
					throw new ParseException("Can not find serializer for type: " + jwt.getTkt(), 0);
				}
				Token deserialize = serializer.deserialize(new ByteArrayInputStream(JWTUtils.decrypt(jwt.getTkn().getBytes(Charset.forName("ASCII")), key)));
				if (deserialize == null) {
					throw new ParseException("Can not recover encrypted token", 1);
				}
				context.put(tokenKey, deserialize);
			}
			else {
				context.put(tokenKey, new JWTToken(jwt));
			}
		}
		
		// generate with an existing non-jwt token
		public JWTSession(Token token) {
			this.id = generateId(token);
			if (token != null) {
				context.put(tokenKey, token);
			}
		}
		
		// generate without authentication
		public JWTSession() {
			this.id = generateId(null);
		}
		
		private String generateId(Token token) {
			if (token == null) {
				return UUID.randomUUID().toString().replace("-", "");
			}
			else {
				JWTBody jwt = new JWTBody();
				jwt.setIat(new Date().getTime() / 1000);
				Date validUntil = token.getValidUntil() == null ? new Date(new Date().getTime() + sessionTimeout) : token.getValidUntil();
				jwt.setExp(validUntil.getTime() / 1000);
				jwt.setRlm(token.getRealm());
				jwt.setSub(token.getName());
				TokenSerializer<Token> serializer = TokenSerializerFactory.getInstance().getSerializer(token);
				if (serializer != null) {
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					serializer.serialize(output, token);
					jwt.setTkt(serializer.getName());
					jwt.setTkn(new String(JWTUtils.encrypt(output.toByteArray(), key), Charset.forName("ASCII")));
				}
				return JWTUtils.encodeJWT(new BeanInstance<JWTBody>(jwt), key);
			}
		}
		
		@Override
		public Iterator<String> iterator() {
			return context.keySet().iterator();
		}
		
		@Override
		public String getId() {
			return id;
		}
		
		@Override
		public Object get(String name) {
			return context.get(name);
		}

		@Override
		public void set(String name, Object value) {
			// you are setting a token in this session, we need to upgrade it
			if (tokenKey.equals(name) && key != null) {
				synchronized(sessions) {
					// only one original id allowed...
					if (originalId != null) {
						sessions.remove(originalId);
					}
					originalId = id;
					id = generateId((Token) value);
					sessions.put(id, this);
				}
			}
			// also set the value if it is a token
			if (value == null) {
				context.remove(name);
			}
			else {
				context.put(name, value);
			}
		}

		@Override
		public void destroy() {
			synchronized(sessions) {
				sessions.remove(id);
				if (originalId != null) {
					sessions.remove(originalId);
				}
			}
		}

		public Date getLastAccessed() {
			return lastAccessed;
		}

		public void setLastAccessed(Date lastAccessed) {
			this.lastAccessed = lastAccessed;
		}
	}
	
	public static class JWTBody {
		private long exp, iat;
		private String iss, sub, aud, jti;
		private String rlm, tkn, tkt;

		public long getExp() {
			return exp;
		}

		public void setExp(long exp) {
			this.exp = exp;
		}

		public long getIat() {
			return iat;
		}

		public void setIat(long iat) {
			this.iat = iat;
		}

		public String getIss() {
			return iss;
		}

		public void setIss(String iss) {
			this.iss = iss;
		}

		public String getSub() {
			return sub;
		}

		public void setSub(String sub) {
			this.sub = sub;
		}

		public String getAud() {
			return aud;
		}

		public void setAud(String aud) {
			this.aud = aud;
		}

		public String getJti() {
			return jti;
		}

		public void setJti(String jti) {
			this.jti = jti;
		}

		public String getRlm() {
			return rlm;
		}

		public void setRlm(String rlm) {
			this.rlm = rlm;
		}

		public String getTkn() {
			return tkn;
		}

		public void setTkn(String tkn) {
			this.tkn = tkn;
		}

		public String getTkt() {
			return tkt;
		}

		public void setTkt(String tkt) {
			this.tkt = tkt;
		}
		
	}
	
	public static class JWTToken implements Token {

		private static final long serialVersionUID = 1L;
		private JWTBody body;
		
		public JWTToken(JWTBody body) {
			this.body = body;
		}
		
		public String getRealm() {
			return body.getRlm();
		}
		
		public String getName() {
			return body.getSub();
		}

		@XmlTransient
		public Date getValidUntil() {
			return new Date(1000l * body.getExp());
		}
		
		@XmlTransient
		@Override
		public List<Principal> getCredentials() {
			return null;
		}
	}

	@Override
	public void prune() {
		synchronized(sessions) {
			Date now = new Date();
			Iterator<JWTSession> iterator = sessions.values().iterator();
			while (iterator.hasNext()) {
				JWTSession session = iterator.next();
				Date timeout = session.get(tokenKey) instanceof JWTToken ? ((JWTToken) session.get(tokenKey)).getValidUntil() : new Date(session.getLastAccessed().getTime() + sessionTimeout);
				// if the session is timed out, remove it
				if (timeout.before(now)) {
					iterator.remove();
				}
			}
		}
	}
}
