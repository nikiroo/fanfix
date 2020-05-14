package be.nikiroo.fanfix.data;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.utils.Progress;

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
		put(json, "fake_cover", meta.isFakeCover());
		put(json, "image_document", meta.isImageDocument());

		put(json, "resume", toJson(meta.getResume()));
		put(json, "tags", new JSONArray(meta.getTags()));

		return json;
	}

	/**
	 * // no image
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
		meta.setFakeCover(getBoolean(json, "fake_cover", false));
		meta.setImageDocument(getBoolean(json, "image_document", false));

		meta.setResume(toChapter(getJson(json, "resume")));
		meta.setTags(toListString(getJsonArr(json, "tags")));

		return meta;
	}

	static public JSONObject toJson(Story story) {
		if (story == null) {
			return null;
		}

		JSONObject json = new JSONObject();
		put(json, "", Story.class.getName());
		put(json, "meta", toJson(story.getMeta()));

		List<JSONObject> chapters = new ArrayList<JSONObject>();
		for (Chapter chap : story) {
			chapters.add(toJson(chap));
		}
		put(json, "chapters", new JSONArray(chapters));

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
	static public Story toStory(JSONObject json) {
		if (json == null) {
			return null;
		}

		Story story = new Story();
		story.setMeta(toMetaData(getJson(json, "meta")));
		story.setChapters(toListChapter(getJsonArr(json, "chapters")));

		return story;
	}

	static public JSONObject toJson(Chapter chap) {
		if (chap == null) {
			return null;
		}

		JSONObject json = new JSONObject();
		put(json, "", Chapter.class.getName());
		put(json, "name", chap.getName());
		put(json, "number", chap.getNumber());
		put(json, "words", chap.getWords());

		List<JSONObject> paragraphs = new ArrayList<JSONObject>();
		for (Paragraph para : chap) {
			paragraphs.add(toJson(para));
		}
		put(json, "paragraphs", new JSONArray(paragraphs));

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

		Chapter chap = new Chapter(getInt(json, "number", 0),
				getString(json, "name"));
		chap.setWords(getLong(json, "words", 0));

		chap.setParagraphs(toListParagraph(getJsonArr(json, "paragraphs")));

		return chap;
	}

	// no images
	static public JSONObject toJson(Paragraph para) {
		if (para == null) {
			return null;
		}

		JSONObject json = new JSONObject();

		put(json, "", Paragraph.class.getName());
		put(json, "content", para.getContent());
		put(json, "words", para.getWords());

		put(json, "type", para.getType().toString());

		return json;
	}

	/**
	 * // no images
	 * 
	 * @param json
	 * 
	 * @return
	 * 
	 * @throws JSONException
	 *             when it cannot be converted
	 */
	static public Paragraph toParagraph(JSONObject json) {
		if (json == null) {
			return null;
		}

		Paragraph para = new Paragraph(
				ParagraphType.valueOf(getString(json, "type")),
				getString(json, "content"), getLong(json, "words", 0));

		return para;
	}

	// no children included
	static public JSONObject toJson(Progress pg) {
		if (pg == null) {
			return null;
		}

		JSONObject json = new JSONObject();

		put(json, "", Progress.class.getName());
		put(json, "name", pg.getName());
		put(json, "min", pg.getMin());
		put(json, "max", pg.getMax());
		put(json, "progress", pg.getProgress());

		return json;
	}

	// no children included
	static public Progress toProgress(JSONObject json) {
		if (json == null) {
			return null;
		}

		Progress pg = new Progress(getString(json, "name"),
				getInt(json, "min", 0), getInt(json, "max", 100));
		pg.setProgress(getInt(json, "progress", 0));

		return pg;
	}

	static public List<String> toListString(JSONArray array) {
		if (array != null) {
			List<String> values = new ArrayList<String>();
			for (int i = 0; i < array.length(); i++) {
				values.add(array.getString(i));
			}
			return values;
		}

		return null;
	}

	static public List<Paragraph> toListParagraph(JSONArray array) {
		if (array != null) {
			List<Paragraph> values = new ArrayList<Paragraph>();
			for (int i = 0; i < array.length(); i++) {
				JSONObject value = array.getJSONObject(i);
				values.add(toParagraph(value));
			}
			return values;
		}

		return null;
	}

	private static List<Chapter> toListChapter(JSONArray array) {
		if (array != null) {
			List<Chapter> values = new ArrayList<Chapter>();
			for (int i = 0; i < array.length(); i++) {
				JSONObject value = array.getJSONObject(i);
				values.add(toChapter(value));
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

	static boolean getBoolean(JSONObject json, String key, boolean def) {
		if (json.has(key)) {
			Object o = json.get(key);
			if (o instanceof Boolean) {
				return (Boolean) o;
			}
		}

		return def;
	}

	static int getInt(JSONObject json, String key, int def) {
		if (json.has(key)) {
			Object o = json.get(key);
			if (o instanceof Integer) {
				return (Integer) o;
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
