package be.nikiroo.utils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Simple proxy helper to select a default internet proxy.
 * 
 * @author niki
 */
public class Proxy {
	/**
	 * Use the system proxy.
	 */
	static public void useSystemProxy() {
		useSystemProxy(null, null);
	}

	/**
	 * Use the system proxy with the given login/password, for authenticated
	 * proxies.
	 * 
	 * @param user
	 *            the user name or login
	 * @param password
	 *            the password
	 */
	static public void useSystemProxy(String user, String password) {
		System.setProperty("java.net.useSystemProxies", "true");
		auth(user, password);
	}

	/**
	 * Use the give proxy.
	 * 
	 * @param host
	 *            the proxy host name or IP address
	 * @param port
	 *            the port to use
	 */
	static public void useProxy(String host, int port) {
		useProxy(host, port, null, null);
	}

	/**
	 * Use the given proxy with the given login/password, for authenticated
	 * proxies.
	 * 
	 * @param user
	 *            the user name or login
	 * @param password
	 *            the password
	 * @param host
	 *            the proxy host name or IP address
	 * @param port
	 *            the port to use
	 * @param user
	 *            the user name or login
	 * @param password
	 *            the password
	 */
	static public void useProxy(String host, int port, String user,
			String password) {
		System.setProperty("http.proxyHost", "proxy.stluc.ucl.ac.be");
		System.setProperty("http.proxyPort", "8080");
		auth(user, password);
	}

	/**
	 * Select the default authenticator for proxy requests.
	 * 
	 * @param user
	 *            the user name or login
	 * @param password
	 *            the password
	 */
	static private void auth(final String user, final String password) {
		if (user != null && password != null) {
			Authenticator proxy = new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					if (getRequestorType() == RequestorType.PROXY) {
						return new PasswordAuthentication(user,
								password.toCharArray());
					}
					return null;
				}
			};
			Authenticator.setDefault(proxy);
		}
	}
}
