package be.nikiroo.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple login facility using cookies.
 * 
 * @author niki
 */
public class LoginResult {
	private boolean success;
	private String cookie;
	private boolean badLogin;
	private boolean badCookie;
	private String option;

	/**
	 * Generate a failed login.
	 * 
	 * @param badLogin
	 *            TRUE if the login failed because of a who/key/subkey error
	 * @param badCookie
	 *            TRUE if the login failed because of a bad cookie
	 * 
	 */
	public LoginResult(boolean badLogin, boolean badCookie) {
		this.badLogin = badLogin;
		this.badCookie = badCookie;
	}

	/**
	 * Generate a successful login for the given user.
	 * 
	 * @param who
	 *            the user (can be NULL)
	 * @param key
	 *            the password (can be NULL)
	 * @param subkey
	 *            a sub-password (can be NULL)
	 * @param option
	 *            an option assigned to this login (can be NULL)
	 */
	public LoginResult(String who, String key, String subkey, String option) {
		this.option = option;
		this.cookie = generateCookie(who, key, subkey, option);
		this.success = true;
	}

	/**
	 * Generate a login via this token and checks its validity.
	 * <p>
	 * Will fail with a NULL <tt>token</tt>, but
	 * {@link LoginResult#isBadCookie()} will still be false.
	 * 
	 * @param cookie
	 *            the token to check (if NULL, will simply fail but
	 *            {@link LoginResult#isBadCookie()} will still be false)
	 * @param who
	 *            the user (can be NULL)
	 * @param key
	 *            the password (can be NULL)
	 */
	public LoginResult(String cookie, String who, String key) {
		this(cookie, who, key, null, true);
	}

	/**
	 * Generate a login via this token and checks its validity.
	 * <p>
	 * Will fail with a NULL <tt>token</tt>, but
	 * {@link LoginResult#isBadCookie()} will still be false.
	 * 
	 * @param cookie
	 *            the token to check (if NULL, will simply fail but
	 *            {@link LoginResult#isBadCookie()} will still be false)
	 * @param who
	 *            the user (can be NULL)
	 * @param key
	 *            the password (can be NULL)
	 * @param subkeys
	 *            the list of candidate subkey (can be NULL)
	 * @param allowNoSubkey
	 *            allow the login if no subkey was present in the token
	 */
	public LoginResult(String cookie, String who, String key,
			List<String> subkeys, boolean allowNoSubkey) {
		if (cookie != null) {
			String hashes[] = cookie.split("~");
			if (hashes.length >= 2) {
				String wookie = hashes[0];
				String rehashed = hashes[1];
				String opts = hashes.length > 2 ? hashes[2] : "";

				if (CookieUtils.validateCookie(who + key, wookie)) {
					if (subkeys == null) {
						subkeys = new ArrayList<String>();
					}

					if (allowNoSubkey) {
						subkeys = new ArrayList<String>(subkeys);
						subkeys.add("");
					}

					for (String subkey : subkeys) {
						if (CookieUtils.validateCookie(wookie + subkey + opts,
								rehashed)) {
							this.cookie = generateCookie(who, key, subkey,
									opts);
							this.option = opts;
							this.success = true;
						}
					}
				}
			}

			this.badCookie = !success;
		}

		// No token -> no bad token
	}

	/**
	 * The login wa successful.
	 * 
	 * @return TRUE if it is
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * The refreshed token if the login is successful (NULL if not).
	 * 
	 * @return the token, or NULL
	 */
	public String getCookie() {
		return cookie;
	}

	/**
	 * An option that was used to generate this login (always NULL if the login
	 * was not successful).
	 * <p>
	 * It can come from a manually generated {@link LoginResult}, but also from
	 * a {@link LoginResult} generated with a token.
	 * 
	 * @return the option
	 */
	public String getOption() {
		return option;
	}

	/**
	 * The login failed because of a who/key/subkey error.
	 * 
	 * @return TRUE if it failed because of a who/key/subkey error
	 */
	public boolean isBadLogin() {
		return badLogin;
	}

	/**
	 * The login failed because the cookie was not accepted
	 * 
	 * @return TRUE if it failed because the cookie was not accepted
	 */
	public boolean isBadCookie() {
		return badCookie;
	}

	@Override
	public String toString() {
		if (success)
			return "Login succeeded";

		if (badLogin && badCookie)
			return "Login failed because of bad login and bad cookie";

		if (badLogin)
			return "Login failed because of bad login";

		if (badCookie)
			return "Login failed because of bad cookie";

		return "Login failed without giving a reason";
	}

	/**
	 * Generate a cookie.
	 * 
	 * @param who
	 *            the user name (can be NULL)
	 * @param key
	 *            the password (can be NULL)
	 * @param subkey
	 *            a subkey (can be NULL)
	 * @param option
	 *            an option linked to the login (can be NULL)
	 * 
	 * @return a fresh cookie
	 */
	private String generateCookie(String who, String key, String subkey,
			String option) {
		String wookie = CookieUtils.generateCookie(who + key, 0);
		return wookie + "~"
				+ CookieUtils.generateCookie(
						wookie + (subkey == null ? "" : subkey) + option, 0)
				+ "~" + option;
	}
}