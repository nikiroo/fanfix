package be.nikiroo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Version checker: can check the current version of the program against a
 * remote changelog, and list the missed updates and their description.
 * 
 * @author niki
 */
public class VersionCheck {
	private static final String base = "https://github.com/${PROJECT}/raw/master/changelog${LANG}.md";
	private static Downloader downloader = new Downloader(null);

	private Version current;
	private List<Version> newer;
	private Map<Version, List<String>> changes;

	/**
	 * Create a new {@link VersionCheck}.
	 * 
	 * @param current
	 *            the current version of the program
	 * @param newer
	 *            the list of available {@link Version}s newer the current one
	 * @param changes
	 *            the list of changes
	 */
	private VersionCheck(Version current, List<Version> newer,
			Map<Version, List<String>> changes) {
		this.current = current;
		this.newer = newer;
		this.changes = changes;
	}

	/**
	 * Check if there are more recent {@link Version}s of this program
	 * available.
	 * 
	 * @return TRUE if there is at least one
	 */
	public boolean isNewVersionAvailable() {
		return !newer.isEmpty();
	}

	/**
	 * The current {@link Version} of the program.
	 * 
	 * @return the current {@link Version}
	 */
	public Version getCurrentVersion() {
		return current;
	}

	/**
	 * The list of available {@link Version}s newer than the current one.
	 * 
	 * @return the newer {@link Version}s
	 */
	public List<Version> getNewer() {
		return newer;
	}

	/**
	 * The list of changes for each available {@link Version} newer than the
	 * current one.
	 * 
	 * @return the list of changes
	 */
	public Map<Version, List<String>> getChanges() {
		return changes;
	}

	/**
	 * Check if there are available {@link Version}s of this program more recent
	 * than the current one.
	 * 
	 * @param githubProject
	 *            the GitHub project to check on, for instance "nikiroo/fanfix"
	 * @param lang
	 *            the current locale, so we can try to get the changelog in the
	 *            correct language (can be NULL, will fetch the default
	 *            changelog)
	 * 
	 * @return a {@link VersionCheck}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static VersionCheck check(String githubProject, Locale lang)
			throws IOException {
		Version current = Version.getCurrentVersion();
		List<Version> newer = new ArrayList<Version>();
		Map<Version, List<String>> changes = new HashMap<Version, List<String>>();

		// Use the right project:
		String base = VersionCheck.base.replace("${PROJECT}", githubProject);

		// Prepare the URLs according to the user's language (we take here
		// "-fr_BE" as an example):
		String fr = lang == null ? "" : "-" + lang.getLanguage();
		String BE = lang == null ? ""
				: "_" + lang.getCountry().replace(".UTF8", "");
		String urlFrBE = base.replace("${LANG}", fr + BE);
		String urlFr = base.replace("${LANG}", "-" + fr);
		String urlDefault = base.replace("${LANG}", "");

		InputStream in = null;
		for (String url : new String[] { urlFrBE, urlFr, urlDefault }) {
			try {
				in = downloader.open(new URL(url), false);
				break;
			} catch (IOException e) {
			}
		}

		if (in == null) {
			throw new IOException("No changelog found");
		}

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(in, "UTF-8"));
		try {
			Version version = new Version();
			for (String line = reader.readLine(); line != null; line = reader
					.readLine()) {
				if (line.startsWith("## Version ")) {
					version = new Version(
							line.substring("## Version ".length()));
					if (version.isNewerThan(current)) {
						newer.add(version);
						changes.put(version, new ArrayList<String>());
					} else {
						version = new Version();
					}
				} else if (!version.isEmpty() && !newer.isEmpty()
						&& !line.isEmpty()) {
					List<String> ch = changes.get(newer.get(newer.size() - 1));
					if (!ch.isEmpty() && !line.startsWith("- ")) {
						int i = ch.size() - 1;
						ch.set(i, ch.get(i) + " " + line.trim());
					} else {
						ch.add(line.substring("- ".length()).trim());
					}
				}
			}
		} finally {
			reader.close();
		}

		return new VersionCheck(current, newer, changes);
	}
	
	@Override
	public String toString() {
		return String.format(
				"Version checker: version [%s], %d releases behind latest version [%s]", //
				current, //
				newer.size(), //
				newer.isEmpty() ? current : newer.get(newer.size() - 1)//
		);
	}
}
