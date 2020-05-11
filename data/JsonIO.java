package be.nikiroo.fanfix.data;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonIO {
	static public JSONObject toJson(MetaData meta) {
		if (meta == null) {
			return null;
		}

		JSONObject json = new JSONObject();
		put(json, "", MetaData.class.getName());
		put(json, "luid", meta.getLuid());
		put(json, "title", meta.getTitle());
		put(json, "author", meta.getAuthor());
		put(json, "source", meta.getSource());
		put(json, "url", meta.getUrl());
		put(json, "words", meta.getWords());
		put(json, "creation_date", meta.getCreationDate());
		put(json, "date", meta.getDate());
		put(json, "lang", meta.getLang());
		put(json, "publisher", meta.getPublisher());
		put(json, "subject", meta.getSubject());
		put(json, "type", meta.getType());
		put(json, "uuid", meta.getUuid());
		put(json, "resume", toJson(meta.getResume()));
		put(json, "tags", new JSONArray(meta.getTags()));

		return json;
	}

	/**
	 * 
	 * @param json
	 * 
	 * @return
	 * 
	 * @throws JSONException
	 *             when it cannot be converted
	 */
	static public MetaData toMetaData(JSONObject json) {
		if (json == null) {
			return null;
		}

		MetaData meta = new MetaData();
		meta.setLuid(getString(json, "luid"));
		meta.setTitle(getString(json, "title"));
		meta.setAuthor(getString(json, "author"));
		meta.setSource(getString(json, "source"));
		meta.setUrl(getString(json, "url"));
		meta.setWords(getLong(json, "words", 0));
		meta.setCreationDate(getString(json, "creation_date"));
		meta.setDate(getString(json, "date"));
		meta.setLang(getString(json, "lang"));
		meta.setPublisher(getString(json, "publisher"));
		meta.setSubject(getString(json, "subject"));
		meta.setType(getString(json, "type"));
		meta.setUuid(getString(json, "uuid"));

		meta.setResume(toChapter(getJson(json, "resume")));
		meta.setTags(toListString(getJsonArr(json, "tags")));

		return meta;
	}

	static public JSONObject toJson(Chapter chap) {
		if (chap == null) {
			return null;
		}

		JSONObject json = new JSONObject();

		// TODO

		return json;
	}

	/**
	 * 
	 * @param json
	 * 
	 * @return
	 * 
	 * @throws JSONException
	 *             when it cannot be converted
	 */
	static public Chapter toChapter(JSONObject json) {
		if (json == null) {
			return null;
		}

		Chapter chap = new Chapter(0, "");

		// TODO

		return chap;
	}

	static public List<String> toListString(JSONArray array) {
		if (array != null) {
			List<String> values = new ArrayList<String>();
			for (Object value : array.toList()) {
				values.add(value == null ? null : value.toString());
			}
			return values;
		}

		return null;
	}

	static private void put(JSONObject json, String key, Object o) {
		json.put(key, o == null ? JSONObject.NULL : o);
	}

	static String getString(JSONObject json, String key) {
		if (json.has(key)) {
			Object o = json.get(key);
			if (o instanceof String) {
				return (String) o;
			}
		}

		return null;
	}

	static long getLong(JSONObject json, String key, long def) {
		if (json.has(key)) {
			Object o = json.get(key);
			if (o instanceof Long) {
				return (Long) o;
			}
		}

		return def;
	}

	static JSONObject getJson(JSONObject json, String key) {
		if (json.has(key)) {
			Object o = json.get(key);
			if (o instanceof JSONObject) {
				return (JSONObject) o;
			}
		}

		return null;
	}

	static JSONArray getJsonArr(JSONObject json, String key) {
		if (json.has(key)) {
			Object o = json.get(key);
			if (o instanceof JSONArray) {
				return (JSONArray) o;
			}
		}

		return null;
	}
}
