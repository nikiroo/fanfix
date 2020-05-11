package be.nikiroo.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small class to easily hash some values in a few different ways.
 * <p>
 * Does <b>not</b> handle the salt itself, you have to add it yourself.
 * 
 * @author niki
 */
public class HashUtils {
	/**
	 * Hash the given value.
	 * 
	 * @param value
	 *            the value to hash
	 * 
	 * @return the hash that can be used to confirm a value
	 * 
	 * @throws RuntimeException
	 *             if UTF-8 support is not available (!) or SHA-512 support is
	 *             not available
	 * @throws NullPointerException
	 *             if email or pass is NULL
	 */
	static public String sha512(String value) {
		return hash("SHA-512", value);
	}

	/**
	 * Hash the given value.
	 * 
	 * @param value
	 *            the value to hash
	 * 
	 * @return the hash that can be used to confirm the a value
	 * 
	 * @throws RuntimeException
	 *             if UTF-8 support is not available (!) or MD5 support is not
	 *             available
	 * @throws NullPointerException
	 *             if email or pass is NULL
	 */
	static public String md5(String value) {
		return hash("MD5", value);
	}

	/**
	 * Hash the given value.
	 * 
	 * @param algo
	 *            the hash algorithm to use ("MD5" and "SHA-512" are supported)
	 * @param value
	 *            the value to hash
	 * 
	 * @return the hash that can be used to confirm a value
	 * 
	 * @throws RuntimeException
	 *             if UTF-8 support is not available (!) or the algorithm
	 *             support is not available
	 * @throws NullPointerException
	 *             if email or pass is NULL
	 */
	static private String hash(String algo, String value) {
		try {
			MessageDigest md = MessageDigest.getInstance(algo);
			md.update(value.getBytes("UTF-8"));
			byte byteData[] = md.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < byteData.length; i++) {
				String hex = Integer.toHexString(0xff & byteData[i]);
				if (hex.length() % 2 == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(algo + " hashing not available", e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(
					"UTF-8 encoding is required in a compatible JVM", e);
		}
	}
}
