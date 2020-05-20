package be.nikiroo.fanfix.library;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.streams.ReplaceInputStream;

public class Template {
	private Class<?> location;
	private String name;

	private Map<String, String> values = new HashMap<String, String>();
	private Map<String, Template> valuesTemplate = new HashMap<String, Template>();
	private Map<String, List<Template>> valuesTemplateList = new HashMap<String, List<Template>>();

	public Template(Class<?> location, String name) {
		this.location = location;
		this.name = name;
	}

	public synchronized InputStream read() throws IOException {

		String from[] = new String[values.size() + valuesTemplate.size()
				+ valuesTemplateList.size()];
		String to[] = new String[from.length];

		int i = 0;

		for (String key : values.keySet()) {
			from[i] = "${" + key + "}";
			to[i] = values.get(key);

			i++;
		}
		for (String key : valuesTemplate.keySet()) {
			InputStream value = valuesTemplate.get(key).read();
			try {
				from[i] = "${" + key + "}";
				to[i] = IOUtils.readSmallStream(value);
			} finally {
				value.close();
			}

			i++;
		}
		for (String key : valuesTemplateList.keySet()) {
			List<Template> templates = valuesTemplateList.get(key);
			StringBuilder value = new StringBuilder();
			for (Template template : templates) {
				InputStream valueOne = template.read();
				try {
					value.append(IOUtils.readSmallStream(valueOne));
				} finally {
					valueOne.close();
				}
			}

			from[i] = "${" + key + "}";
			to[i] = value.toString();

			i++;
		}

		InputStream in = IOUtils.openResource(location, name);
		return new ReplaceInputStream(in, from, to);
	}

	public synchronized Template set(String key, String value) {
		values.put(key, value == null ? "" : value);
		valuesTemplate.remove(key);
		valuesTemplateList.remove(key);
		return this;
	}

	public synchronized Template set(String key, Template value) {
		if (value == null) {
			return set(key, "");
		}

		values.remove(key);
		valuesTemplate.put(key, value);
		valuesTemplateList.remove(key);

		return this;
	}

	public synchronized Template set(String key, List<Template> value) {
		values.remove(key);
		valuesTemplate.remove(key);
		valuesTemplateList.put(key, value);
		return this;
	}

	@Override
	public String toString() {
		return String.format(
				"[Template for %s with (%d,%d,%d) value(s) to replace]", name,
				values.size(), valuesTemplate.size(),
				valuesTemplateList.size());
	}
}
