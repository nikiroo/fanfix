package be.nikiroo.fanfix.bundles;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import be.nikiroo.utils.resources.TransBundle;

/**
 * This class manages the translation resources of the application.
 * 
 * @author niki
 */
public class StringIdBundle extends TransBundle<StringId> {
	private String lang;

	/**
	 * Create a translation service for the given language (will fall back to
	 * the default one i not found).
	 * 
	 * @param lang
	 *            the language to use
	 */
	public StringIdBundle(String lang) {
		super(StringId.class, Target.resources, lang);
		this.lang = lang;
	}

	/**
	 * Return the currently used language as a String.
	 * 
	 * @return the language
	 * 
	 * @deprecated use the call from {@link TransBundle} when available
	 */
	public Locale getLanguage() {
		return getLocaleFor(lang);
	}

	/**
	 * Update resource file.
	 * 
	 * @param args
	 *            not used
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void main(String[] args) throws IOException {
		String path = new File(".").getAbsolutePath()
				+ "/src/be/nikiroo/fanfix/bundles/";
		new StringIdBundle(null).updateFile(path);
		System.out.println("Path updated: " + path);
	}

	/**
	 * Return the {@link Locale} representing the given language.
	 * 
	 * @param language
	 *            the language to initialise, in the form "en-GB" or "fr" for
	 *            instance
	 * 
	 * @return the corresponding {@link Locale} or the default {@link Locale} if
	 *         it is not known
	 * 
	 * @deprecated Use the call from {@link TransBundle} when available.
	 */
	static private Locale getLocaleFor(String language) {
		Locale locale;

		if (language == null) {
			locale = Locale.getDefault();
		} else {
			language = language.replaceAll("_", "-");
			String lang = language;
			String country = null;
			if (language.contains("-")) {
				lang = language.split("-")[0];
				country = language.split("-")[1];
			}

			if (country != null)
				locale = new Locale(lang, country);
			else
				locale = new Locale(lang);
		}

		return locale;
	}
}
