package be.nikiroo.fanfix.library.web.templates;

import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.library.Template;
import be.nikiroo.utils.Version;

/**
 * Utility class to retrieve and fill HTML templates for Fanfix web server.
 * 
 * @author niki
 */
public class WebLibraryServerTemplates {
	static private WebLibraryServerTemplates instance = new WebLibraryServerTemplates();

	/**
	 * Get the (unique) instance of this {@link WebLibraryServerTemplates}.
	 * 
	 * @return the (unique) instance
	 */
	static public WebLibraryServerTemplates getInstance() {
		return instance;
	}

	public Template bookline(String luid, String href, String title,
			String author, boolean cached) {

		String cachedClass = "cached";
		String cachedValue = "&#9673;";
		if (!cached) {
			cachedClass = "uncached";
			cachedValue = "&#9675;";
		}

		return new Template(getClass(), "bookline.html") //
				.set("href", href) //
				.set("cachedClass", cachedClass) //
				.set("cached", cachedValue) //
				.set("luid", luid) //
				.set("title", title) //
				.set("author", author) //
		;
	}

	public Template index(boolean banner, boolean fullscreen,
			List<Template> content) {
		String favicon = "favicon.ico";
		String icon = Instance.getInstance().getUiConfig()
				.getString(UiConfig.PROGRAM_ICON);
		if (icon != null) {
			favicon = "icon_" + icon.replace("-", "_") + ".png";
		}

		Template index = new Template(getClass(), "index.html") //
				.set("title", "Fanfix") //
				.set("favicon", favicon) //
				.set("mainClass", fullscreen ? "fullscreen" : "") //
				.set("content", content) //
		;

		if (banner) {
			index.set("banner", new Template(getClass(), "index.banner.html") //
					.set("favicon", favicon) //
					.set("version", Version.getCurrentVersion().toString()) //
			);
		} else {
			index.set("banner", "");
		}

		return index;
	}

	public Template login(String url) {
		return new Template(getClass(), "login.html") //
				.set("url", url) //
		;
	}

	public Template message(String message, boolean error) {
		return new Template(getClass(), "message.html") //
				.set("class", error ? "message error" : "message") //
				.set("message", message) //
		;
	}

	public Template browser(String selectedValue, String filter,
			List<Template> selects) {
		return new Template(getClass(), "browser.html") //
				.set("sourcesSelected",
						"sources".equals(selectedValue) ? "selected='selected'"
								: "") //
				.set("authorsSelected",
						"authors".equals(selectedValue) ? "selected='selected'"
								: "") //
				.set("tagsSelected",
						"tags".equals(selectedValue) ? "selected='selected'"
								: "") //
				.set("filter", filter) //
				.set("selects", selects) //
		;
	}

	public Template browserOption(String name, String value,
			String selectedValue) {
		return new Template(getClass(), "browser.option.html") //
				.set("value", value) //
				.set("selected",
						value.equals(selectedValue) ? "selected='selected'"
								: "") //
				.set("name", name) //
		;
	}

	public Template browserSelect(String name, String value,
			List<Template> options) {
		return new Template(getClass(), "browser.select.html") //
				.set("name", name) //
				.set("value", value) //
				.set("options", options) //
		;
	}

	public Template viewerDesc(String title, String href, String cover,
			List<Template> desclines) {
		return new Template(getClass(), "viewer.desc.html") //
				.set("title", title) //
				.set("href", href) //
				.set("cover", cover) //
				.set("details", desclines) //
		;
	}

	public Template viewerDescline(String key, String value) {
		return new Template(getClass(), "viewer.descline.html") //
				.set("key", key) //
				.set("value", value) //
		;
	}

	// href NULL means no forward link
	public Template viewerImage(String src, String href, String zoomStyle) {
		return new Template(getClass(),
				href == null ? "viewer.image.nolink.html" : "viewer.image.html") //
						.set("src", src) //
						.set("href", href) //
						.set("zoomStyle", zoomStyle) //
		;
	}

	public Template viewerText(Template desc, String content) {
		return new Template(getClass(), "viewer.text.html") //
				.set("desc", desc) //
				.set("content", content) //
		;
	}

	public Template viewerLink(String name, String link, boolean selected) {
		return new Template(getClass(), "viewer.link.html") //
				.set("link", link) //
				.set("class", selected ? "selected" : "") //
				.set("name", name) //
		;
	}

	public Template viewerNavbar(int current, List<Template> links,
			String hrefFirst, String hrefPrevious, String hrefNext,
			String hrefLast, boolean disabledFirst, boolean disabledPrevious,
			boolean disabledNext, boolean disabledLast) {
		return new Template(getClass(), "viewer.navbar.html") //
				.set("disabledFirst",
						disabledFirst ? "disabled='disabled'" : "") //
				.set("disabledPrevious",
						disabledPrevious ? "disabled='disabled'" : "") //
				.set("disabledNext", disabledNext ? "disabled='disabled'" : "") //
				.set("disabledLast", disabledLast ? "disabled='disabled'" : "") //
				.set("hrefFirst", hrefFirst) //
				.set("hrefPrevious", hrefPrevious) //
				.set("hrefNext", hrefNext) //
				.set("hrefLast", hrefLast) //
				.set("current", Integer.toString(current)) //
				.set("links", links) //
		;
	}

	// supported numberOfButtons = 5 or 4 or 1 or the moment
	public Template viewerOptionbar(int numberOfButtons,
			List<Template> buttons) {
		return new Template(getClass(), "viewer.optionbar.html") //
				.set("classSize", "s" + numberOfButtons) //
				.set("buttons", buttons) //
		;
	}

	public Template viewerOptionbarButton(String value, String href,
			String className, boolean disabled) {
		return new Template(getClass(), "viewer.optionbar.button.html") //
				.set("disabled", disabled ? "disabled='disabled'" : "") //
				.set("class", className) //
				.set("href", href) //
				.set("value", value) //
		;
	}
}
