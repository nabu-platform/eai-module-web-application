package be.nabu.eai.module.web.application.jwt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.utils.codec.TranscoderUtils;
import be.nabu.utils.codec.impl.Base64Decoder;
import be.nabu.utils.codec.impl.Base64Encoder;
import be.nabu.utils.io.IOUtils;

public class JWTUtils {
	
	public enum JWTAlgorithm {
		SHA256("HmacSHA256", "HS256")
		;
		
		private String name;
		private String jsonName;

		private JWTAlgorithm(String name, String jsonName) {
			this.name = name;
			this.jsonName = jsonName;
		}

		public String getName() {
			return name;
		}

		public String getJsonName() {
			return jsonName;
		}
		
	}

	public static ComplexContent decodeJWT(ComplexType type, String content, SecretKey key) throws ParseException {
		String[] parts = content.split("\\.");
		if (parts.length != 3) {
			throw new ParseException("Expecting three parts in the token: " + content, 0);
		}
		try {
			byte [] base64Header = parts[0].getBytes("ASCII");
			byte [] base64Body = parts[1].getBytes("ASCII");
			
			ComplexContent header = unmarshal((ComplexType) BeanResolver.getInstance().resolve(Header.class), base64Header);
			String alg = (String) header.get("alg");
			String typ = (String) header.get("typ");
			if (!"JWT".equalsIgnoreCase(typ)) {
				throw new ParseException("Expecting JWT type tokens only: " + typ, 1);
			}
			if (!JWTAlgorithm.SHA256.getJsonName().equalsIgnoreCase(alg)) {
				throw new ParseException("Only HS256 algorithm supported currently: " + alg, 2);
			}
			
			if (key != null) {
				Mac mac = Mac.getInstance(JWTAlgorithm.SHA256.getName());
				mac.init(key);
				mac.update(base64Header);
				mac.update((byte) '.');
				mac.update(base64Body);
				
				byte[] correctSignature = base64Encode(mac.doFinal());
				
				if (!new String(correctSignature, "ASCII").equals(parts[2])) {
					throw new ParseException("Invalid signature", 3);
				}
			}
			
			return unmarshal(type, base64Body);
		}
		catch (ParseException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] encrypt(byte [] content, SecretKey key) {
		try {
			Cipher cipher = Cipher.getInstance(key.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] encrypted = cipher.doFinal(content);
			return base64Encode(encrypted);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] decrypt(byte [] content, SecretKey key) {
		try {
			Cipher cipher = Cipher.getInstance(key.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(base64Decode(content));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String encodeJWT(ComplexContent content, SecretKey key) {
		try {
			// marshal the header
			Header header = new Header();
			header.setAlg(JWTAlgorithm.SHA256.getJsonName());
			header.setTyp("JWT");
			byte [] base64Header = marshal(new BeanInstance<Header>(header));
			byte [] base64Body = marshal(content);

			Mac mac = Mac.getInstance(JWTAlgorithm.SHA256.getName());
			mac.init(key);
			mac.update(base64Header);
			mac.update((byte) '.');
			mac.update(base64Body);
			
			byte[] signature = base64Encode(mac.doFinal());
			
			return new String(base64Header, "ASCII") + "." + new String(base64Body, "ASCII") + "." + new String(signature, "ASCII");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static byte[] marshal(ComplexContent content) throws IOException {
		JSONBinding binding = new JSONBinding(content.getType(), Charset.forName("UTF-8"));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		binding.marshal(output, content);
		return base64Encode(output.toByteArray());
	}
	
	private static ComplexContent unmarshal(ComplexType type, byte [] content) throws IOException, ParseException {
		JSONBinding binding = new JSONBinding(type, Charset.forName("UTF-8"));
		return binding.unmarshal(new ByteArrayInputStream(base64Decode(content)), new Window[0]);
	}
	
	private static byte[] base64Encode(byte [] content) throws IOException {
		Base64Encoder transcoder = new Base64Encoder();
		transcoder.setBytesPerLine(0);
		return IOUtils.toBytes(TranscoderUtils.transcodeBytes(IOUtils.wrap(content, true), transcoder));
	}
	
	private static byte[] base64Decode(byte [] content) throws IOException {
		return IOUtils.toBytes(TranscoderUtils.transcodeBytes(IOUtils.wrap(content, true), new Base64Decoder()));
	}
	
	public static byte[] encode(byte [] content, SecretKey key, JWTAlgorithm algorithm) {
		try {
			Mac mac = Mac.getInstance(algorithm.getName());
			mac.init(key);
			return mac.doFinal(content);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class Header {
		private String alg, typ;
		
		public String getAlg() {
			return alg;
		}

		public void setAlg(String alg) {
			this.alg = alg;
		}

		public String getTyp() {
			return typ;
		}

		public void setTyp(String typ) {
			this.typ = typ;
		}

	}
}
