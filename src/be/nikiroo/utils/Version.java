package be.nikiroo.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class describe a program {@link Version}.
 * 
 * @author niki
 */
public class Version implements Comparable<Version> {
	private String version;
	private int major;
	private int minor;
	private int patch;

	/**
	 * Create a new, empty {@link Version}.
	 * 
	 */
	public Version() {
	}

	/**
	 * Create a new {@link Version} with the given values.
	 * 
	 * @param major
	 *            the major version
	 * @param minor
	 *            the minor version
	 * @param patch
	 *            the patch version
	 */
	public Version(int major, int minor, int patch) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.version = String.format("%d.%d.%d", major, minor, patch);
	}

	/**
	 * Create a new {@link Version} with the given value, which must be in the
	 * form <tt>MAJOR.MINOR.PATCH</tt>.
	 * 
	 * @param version
	 *            the version (<tt>MAJOR.MINOR.PATCH</tt>)
	 */
	public Version(String version) {
		try {
			String[] tab = version.split("\\.");
			this.major = Integer.parseInt(tab[0]);
			this.minor = Integer.parseInt(tab[1]);
			this.patch = Integer.parseInt(tab[2]);
			this.version = version;
		} catch (Exception e) {
			this.major = 0;
			this.minor = 0;
			this.patch = 0;
			this.version = null;
		}
	}

	/**
	 * The 'major' version.
	 * <p>
	 * This version should only change when API-incompatible changes are made to
	 * the program.
	 * 
	 * @return the major version
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * The 'minor' version.
	 * <p>
	 * This version should only change when new, backwards-compatible
	 * functionality has been added to the program.
	 * 
	 * @return the minor version
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * The 'patch' version.
	 * <p>
	 * This version should change when backwards-compatible bugfixes have been
	 * added to the program.
	 * 
	 * @return the patch version
	 */
	public int getPatch() {
		return patch;
	}

	/**
	 * Check if this {@link Version} is "empty" (i.e., the version was not
	 * parse-able or not given).
	 * <p>
	 * An empty {@link Version} is always <tt>0.0.0</tt>.
	 * 
	 * @return TRUE if it is empty
	 */
	public boolean isEmpty() {
		return major == 0 && minor == 0 && patch == 0;
	}

	/**
	 * Check if we are more recent than the given {@link Version}.
	 * 
	 * @param o
	 *            the other {@link Version}
	 * @return TRUE if this {@link Version} is more recent than the given one
	 */
	public boolean isNewerThan(Version o) {
		if (major > o.major) {
			return true;
		}

		if (major == o.major && minor > o.minor) {
			return true;
		}

		if (major == o.major && minor == o.minor && patch > o.patch) {
			return true;
		}

		return false;
	}

	/**
	 * Check if we are older than the given {@link Version}.
	 * 
	 * @param o
	 *            the other {@link Version}
	 * @return TRUE if this {@link Version} is older than the given one
	 */
	public boolean isOlderThan(Version o) {
		return !equals(o) && !isNewerThan(o);
	}

	/**
	 * Return the version of the running program if it follows the VERSION
	 * convention (i.e., if it has a file called VERSION containing the version
	 * as a {@link String} in its binary root, and if this {@link String}
	 * follows the Major/Minor/Patch convention).
	 * <p>
	 * If it does not, return an empty {@link Version} object.
	 * 
	 * @return the {@link Version} of the program, or an empty {@link Version}
	 *         (does not return NULL)
	 */
	public static Version getCurrentVersion() {
		String version = null;

		InputStream in = IOUtils.openResource("VERSION");
		if (in != null) {
			try {
				ByteArrayOutputStream ba = new ByteArrayOutputStream();
				IOUtils.write(in, ba);
				in.close();

				version = ba.toString("UTF-8").trim();
			} catch (IOException e) {
			}
		}

		return new Version(version);
	}

	public int compareTo(Version o) {
		if (equals(o)) {
			return 0;
		} else if (isNewerThan(o)) {
			return 1;
		} else {
			return -1;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Version) {
			Version o = (Version) obj;
			return o.major == major && o.minor == minor && o.patch == patch;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return version == null ? 0 : version.hashCode();
	}

	/**
	 * Return a user-readable form of this {@link Version}.
	 */
	@Override
	public String toString() {
		return version == null ? "[unknown]" : version;
	}
}
