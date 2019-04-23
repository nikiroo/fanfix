package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;

/**
 * Small utility class to do AES encryption/decryption.
 * <p>
 * Do not assume it is actually secure until you checked the code...
 * 
 * @author niki
 */
public class CryptUtils {
	private Cipher ecipher;
	private Cipher dcipher;

	/**
	 * Small and leazy way to initialize a 128 bits key with {@link CryptUtils}.
	 * <p>
	 * <b>Some</b> part of the key will be used to generate a 128 bits key and
	 * initialize the {@link CryptUtils}; even NULL will generate something.
	 * <p>
	 * <b>This is most probably not secure. Do not use if you actually care
	 * about security.</b>
	 * 
	 * @param key
	 *            the {@link String} to use as a base for the key, can be NULL
	 */
	public CryptUtils(String key) {
		try {
			byte[] bytes32 = key2key(key);
			init(bytes32);
			for (int i = 0 ; i < bytes32.length ; i++) {
				bytes32[i] = 0;
			}
		} catch (InvalidKeyException e) {
			// We made sure that the key is correct, so nothing here
			e.printStackTrace();
		}
	}

	/**
	 * Create a new instance of {@link CryptUtils} with the given 128 bytes key.
	 * <p>
	 * The key <b>must</b> be exactly 128 bytes long.
	 * 
	 * @param bytes32
	 *            the 128 bits (32 bytes) of the key
	 * 
	 * @throws InvalidKeyException
	 *             if the key is not an array of 128 bytes
	 */
	public CryptUtils(byte[] bytes32) throws InvalidKeyException {
		init(bytes32);
		for (int i = 0 ; i < bytes32.length ; i++) {
			bytes32[i] = 0;
		}
	}

	/**
	 * Wrap the given {@link InputStream} so it is transparently encrypted by
	 * the current {@link CryptUtils}.
	 * 
	 * @param in
	 *            the {@link InputStream} to wrap
	 * @return the auto-encode {@link InputStream}
	 */
	public InputStream encryptInputStream(InputStream in) {
		return new CipherInputStream(in, ecipher);
	}

	/**
	 * Wrap the given {@link OutputStream} so it is transparently encrypted by
	 * the current {@link CryptUtils}.
	 * 
	 * @param in
	 *            the {@link OutputStream} to wrap
	 * @return the auto-encode {@link OutputStream}
	 */
	public OutputStream encryptOutpuStream(OutputStream out) {
		return new CipherOutputStream(out, ecipher);
	}

	/**
	 * Wrap the given {@link OutStream} so it is transparently decoded by the
	 * current {@link CryptUtils}.
	 * 
	 * @param in
	 *            the {@link InputStream} to wrap
	 * @return the auto-decode {@link InputStream}
	 */
	public InputStream decryptInputStream(InputStream in) {
		return new CipherInputStream(in, dcipher);
	}

	/**
	 * Wrap the given {@link OutStream} so it is transparently decoded by the
	 * current {@link CryptUtils}.
	 * 
	 * @param out
	 *            the {@link OutputStream} to wrap
	 * @return the auto-decode {@link OutputStream}
	 */
	public OutputStream decryptOutputStream(OutputStream out) {
		return new CipherOutputStream(out, dcipher);
	}

	/**
	 * This method required an array of 128 bytes.
	 * 
	 * @param bytes32
	 *            the array, which <b>must</b> be of 128 bits (32 bytes)
	 * 
	 * @throws InvalidKeyException
	 *             if the key is not an array of 128 bits (32 bytes)
	 */
	private void init(byte[] bytes32) throws InvalidKeyException {
		if (bytes32 == null || bytes32.length != 32) {
			throw new InvalidKeyException(
					"The size of the key must be of 128 bits (32 bytes), it is: "
							+ (bytes32 == null ? "null" : "" + bytes32.length)
							+ " bytes");
		}

		SecretKey key = new SecretKeySpec(bytes32, "AES");
		try {
			ecipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			dcipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			ecipher.init(Cipher.ENCRYPT_MODE, key);
			dcipher.init(Cipher.DECRYPT_MODE, key);
		} catch (NoSuchAlgorithmException e) {
			// Every implementation of the Java platform is required to support
			// this standard Cipher transformation with 128 bits keys
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// Every implementation of the Java platform is required to support
			// this standard Cipher transformation with 128 bits keys
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// Every implementation of the Java platform is required to support
			// this standard Cipher transformation with 128 bits keys
			e.printStackTrace();
		}
	}

	/**
	 * Encrypt the data.
	 * 
	 * @param data
	 *            the data to encrypt
	 * 
	 * @return the encrypted data
	 * 
	 * @throws SSLException
	 *             in case of I/O error (i.e., the data is not what you assumed
	 *             it was)
	 */
	public byte[] encrypt(byte[] data) throws SSLException {
		try {
			return ecipher.doFinal(data);
		} catch (IllegalBlockSizeException e) {
			throw new SSLException(e);
		} catch (BadPaddingException e) {
			throw new SSLException(e);
		}
	}

	/**
	 * Encrypt the data.
	 * 
	 * @param data
	 *            the data to encrypt
	 * 
	 * @return the encrypted data
	 * 
	 * @throws SSLException
	 *             in case of I/O error (i.e., the data is not what you assumed
	 *             it was)
	 */
	public byte[] encrypt(String data) throws SSLException {
		try {
			return encrypt(data.getBytes("UTF8"));
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is required in all confirm JVMs
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Encrypt the data, then encode it into Base64.
	 * 
	 * @param data
	 *            the data to encrypt
	 * @param zip
	 *            TRUE to also compress the data in GZIP format; remember that
	 *            compressed and not-compressed content are different; you need
	 *            to know which is which when decoding
	 * 
	 * @return the encrypted data, encoded in Base64
	 * 
	 * @throws SSLException
	 *             in case of I/O error (i.e., the data is not what you assumed
	 *             it was)
	 */
	public String encrypt64(String data, boolean zip) throws SSLException {
		try {
			return encrypt64(data.getBytes("UTF8"), zip);
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is required in all confirm JVMs
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Encrypt the data, then encode it into Base64.
	 * 
	 * @param data
	 *            the data to encrypt
	 * @param zip
	 *            TRUE to also compress the data in GZIP format; remember that
	 *            compressed and not-compressed content are different; you need
	 *            to know which is which when decoding
	 * 
	 * @return the encrypted data, encoded in Base64
	 * 
	 * @throws SSLException
	 *             in case of I/O error (i.e., the data is not what you assumed
	 *             it was)
	 */
	public String encrypt64(byte[] data, boolean zip) throws SSLException {
		try {
			return StringUtils.base64(encrypt(data), zip);
		} catch (IOException e) {
			// not exactly true, but we consider here that this error is a crypt
			// error, not a normal I/O error
			throw new SSLException(e);
		}
	}

	/**
	 * Decode the data which is assumed to be encrypted with the same utilities.
	 * 
	 * @param data
	 *            the encrypted data to decode
	 * 
	 * @return the original, decoded data
	 * 
	 * @throws SSLException
	 *             in case of I/O error
	 */
	public byte[] decrypt(byte[] data) throws SSLException {
		try {
			return dcipher.doFinal(data);
		} catch (IllegalBlockSizeException e) {
			throw new SSLException(e);
		} catch (BadPaddingException e) {
			throw new SSLException(e);
		}
	}

	/**
	 * Decode the data which is assumed to be encrypted with the same utilities
	 * and to be a {@link String}.
	 * 
	 * @param data
	 *            the encrypted data to decode
	 * 
	 * @return the original, decoded data,as a {@link String}
	 * 
	 * @throws SSLException
	 *             in case of I/O error
	 */
	public String decrypts(byte[] data) throws SSLException {
		try {
			return new String(decrypt(data), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is required in all confirm JVMs
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Decode the data which is assumed to be encrypted with the same utilities
	 * and is a Base64 encoded value.
	 * 
	 * @param data
	 *            the encrypted data to decode in Base64 format
	 * @param zip
	 *            TRUE to also uncompress the data from a GZIP format
	 *            automatically; if set to FALSE, zipped data can be returned
	 * 
	 * @return the original, decoded data
	 * 
	 * @throws SSLException
	 *             in case of I/O error
	 */
	public byte[] decrypt64(String data, boolean zip) throws SSLException {
		try {
			return decrypt(StringUtils.unbase64(data, zip));
		} catch (IOException e) {
			// not exactly true, but we consider here that this error is a crypt
			// error, not a normal I/O error
			throw new SSLException(e);
		}
	}

	/**
	 * Decode the data which is assumed to be encrypted with the same utilities
	 * and is a Base64 encoded value, then convert it into a String (this method
	 * assumes the data <b>was</b> indeed a UTF-8 encoded {@link String}).
	 * 
	 * @param data
	 *            the encrypted data to decode in Base64 format
	 * @param zip
	 *            TRUE to also uncompress the data from a GZIP format
	 *            automatically; if set to FALSE, zipped data can be returned
	 * 
	 * @return the original, decoded data
	 * 
	 * @throws SSLException
	 *             in case of I/O error
	 */
	public String decrypt64s(String data, boolean zip) throws SSLException {
		try {
			return new String(decrypt(StringUtils.unbase64(data, zip)), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is required in all confirm JVMs
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// not exactly true, but we consider here that this error is a crypt
			// error, not a normal I/O error
			throw new SSLException(e);
		}
	}

	/**
	 * This is probably <b>NOT</b> secure!
	 * 
	 * @param input
	 *            some {@link String} input
	 * 
	 * @return a 128 bits key computed from the given input
	 */
	static private byte[] key2key(String input) {
		return StringUtils.getMd5Hash("" + input).getBytes();
	}
}
