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

	// only supported option: a MetaData called "meta"
	static public JSONObject toJson(Progress pg) {
		return toJson(pg, null);
	}

	// only supported option: a MetaData called "meta"
	static private JSONObject toJson(Progress pg, Double weight) {
		if (pg == null) {
			return null;
		}

		// Supported keys: meta (only keep the key on the main parent, where
		// weight is NULL)
		MetaData meta = null;
		if (weight == null) {
			Object ometa = pg.get("meta");
			if (ometa instanceof MetaData) {
				meta = getMetaLight((MetaData) ometa);
			}
		}
		//

		JSONObject json = new JSONObject();

		put(json, "", Progress.class.getName());
		put(json, "name", pg.getName());
		put(json, "min", pg.getMin());
		put(json, "max", pg.getMax());
		put(json, "progress", pg.getRelativeProgress());
		put(json, "weight", weight);
		put(json, "meta", meta);

		List<JSONObject> children = new ArrayList<JSONObject>();
		for (Progress child : pg.getChildren()) {
			children.add(toJson(child, pg.getWeight(child)));
		}
		put(json, "children", new JSONArray(children));

		return json;
	}

	// only supported option: a MetaData called "meta"
	static public Progress toProgress(JSONObject json) {
		if (json == null) {
			return null;
		}

		Progress pg = new Progress( //
				getString(json, "name"), //
				getInt(json, "min", 0), //
				getInt(json, "max", 100) //
		);

		pg.setRelativeProgress(getDouble(json, "progress", 0));

		Object meta = getObject(json, "meta");
		if (meta != null) {
			pg.put("meta", meta);
		}

		JSONArray jchildren = getJsonArr(json, "children");
		for (int i = 0; i < jchildren.length(); i++) {
			try {
				JSONObject jchild = jchildren.getJSONObject(i);
				Double weight = getDouble(jchild, "weight", 0);
				pg.addProgress(toProgress(jchild), weight);
			} catch (Exception e) {
			}
		}

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

	static private List<Chapter> toListChapter(JSONArray array) {
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

	static private Object getObject(JSONObject json, String key) {
		if (json.has(key)) {
			try {
				return json.get(key);
			} catch (Exception e) {
				// Can fail if content was NULL!
			}
		}

		return null;
	}

	static private String getString(JSONObject json, String key) {
		Object o = getObject(json, key);
		if (o instanceof String)
			return (String) o;

		return null;
	}

	static private long getLong(JSONObject json, String key, long def) {
		Object o = getObject(json, key);
		if (o instanceof Byte)
			return (Byte) o;
		if (o instanceof Short)
			return (Short) o;
		if (o instanceof Integer)
			return (Integer) o;
		if (o instanceof Long)
			return (Long) o;

		return def;
	}

	static private double getDouble(JSONObject json, String key, double def) {
		Object o = getObject(json, key);
		if (o instanceof Byte)
			return (Byte) o;
		if (o instanceof Short)
			return (Short) o;
		if (o instanceof Integer)
			return (Integer) o;
		if (o instanceof Long)
			return (Long) o;
		if (o instanceof Float)
			return (Float) o;
		if (o instanceof Double)
			return (Double) o;

		return def;
	}

	static private boolean getBoolean(JSONObject json, String key,
			boolean def) {
		Object o = getObject(json, key);
		if (o instanceof Boolean) {
			return (Boolean) o;
		}

		return def;
	}

	static private int getInt(JSONObject json, String key, int def) {
		Object o = getObject(json, key);
		if (o instanceof Byte)
			return (Byte) o;
		if (o instanceof Short)
			return (Short) o;
		if (o instanceof Integer)
			return (Integer) o;
		if (o instanceof Long) {
			try {
				return (int) (long) ((Long) o);
			} catch (Exception e) {
			}
		}

		return def;
	}

	static private JSONObject getJson(JSONObject json, String key) {
		Object o = getObject(json, key);
		if (o instanceof JSONObject) {
			return (JSONObject) o;
		}

		return null;
	}

	static private JSONArray getJsonArr(JSONObject json, String key) {
		Object o = getObject(json, key);
		if (o instanceof JSONArray) {
			return (JSONArray) o;
		}

		return null;
	}

	// null -> null
	static private MetaData getMetaLight(MetaData meta) {
		MetaData light = null;
		if (meta != null) {
			if (meta.getCover() == null) {
				light = meta;
			} else {
				light = meta.clone();
				light.setCover(null);
			}
		}

		return light;
	}
}
