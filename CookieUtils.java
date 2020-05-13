package be.nikiroo.utils;

import java.util.Date;

/**
 * Some utilities for cookie management.
 * 
 * @author niki
 */
public class CookieUtils {
	/**
	 * The number of seconds for the period (we accept the current or the
	 * previous period as valid for a cookie, via "offset").
	 */
	static public int GRACE_PERIOD = 3600 * 1000; // between 1 and 2h

	/**
	 * Generate a new cookie value from the user (email) and an offset.
	 * <p>
	 * You should use an offset of "0" when creating the cookie, and an offset
	 * of "0" or "-1" if required when checking for the value (the idea is to
	 * allow a cookie to persist across two timespans; if not, the cookie will
	 * be expired the very second we switch to a new timespan).
	 * 
	 * @param value
	 *            the value to generate a cookie for -- you must be able to
	 *            regenerate it in order to check it later
	 * @param offset
	 *            the offset (should be 0 for creating, 0 then -1 if needed for
	 *            checking)
	 * 
	 * @return the new cookie
	 */
	static public String generateCookie(String value, int offset) {
		long unixTime = (long) Math.floor(new Date().getTime() / GRACE_PERIOD)
				+ offset;
		return HashUtils.sha512(value + Long.toString(unixTime));
	}

	/**
	 * Check the given cookie.
	 * 
	 * @param value
	 *            the value to generate a cookie for -- you must be able to
	 *            regenerate it in order to check it later
	 * @param cookie
	 *            the cookie to validate
	 * 
	 * @return TRUE if it is correct
	 */
	static public boolean validateCookie(String value, String cookie) {
		if (cookie != null)
			cookie = cookie.trim();

		String newCookie = generateCookie(value, 0);
		if (!newCookie.equals(cookie)) {
			newCookie = generateCookie(value, -1);
		}

		return newCookie.equals(cookie);
	}
}
