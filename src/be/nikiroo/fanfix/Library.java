package be.nikiroo.fanfix;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;

/**
 * Manage a library of Stories: import, export, list.
 * <p>
 * Each {@link Story} object will be associated with a (local to the library)
 * unique ID, the LUID, which will be used to identify the {@link Story}.
 * 
 * @author niki
 */
public class Library {
	private File baseDir;
	private Map<MetaData, File> stories;
	private int lastId;

	/**
	 * Create a new {@link Library} with the given backend directory.
	 * 
	 * @param dir
	 *            the directoy where to find the {@link Story} objects
	 */
	public Library(File dir) {
		this.baseDir = dir;
		this.stories = new HashMap<MetaData, File>();
		this.lastId = 0;

		dir.mkdirs();
	}

	/**
	 * List all the stories of the given source type in the {@link Library}, or
	 * all the stories if NULL is passed as a type.
	 * 
	 * @param type
	 *            the type of story to retrieve, or NULL for all
	 * 
	 * @return the stories
	 */
	public List<MetaData> getList(SupportType type) {
		String typeString = type == null ? null : type.getSourceName();

		List<MetaData> list = new ArrayList<MetaData>();
		for (Entry<MetaData, File> entry : getStories().entrySet()) {
			String storyType = entry.getValue().getParentFile().getName();
			if (typeString == null || typeString.equalsIgnoreCase(storyType)) {
				list.add(entry.getKey());
			}
		}

		return list;
	}

	/**
	 * Retrieve a specific {@link Story}.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * 
	 * @return the corresponding {@link Story}
	 */
	public Story getStory(String luid) {
		if (luid != null) {
			for (Entry<MetaData, File> entry : getStories().entrySet()) {
				if (luid.equals(entry.getKey().getLuid())) {
					try {
						SupportType type = SupportType.valueOfAllOkUC(entry
								.getKey().getType());
						URL url = entry.getValue().toURI().toURL();
						if (type != null) {
							return BasicSupport.getSupport(type).process(url);
						} else {
							throw new IOException("Unknown type: "
									+ entry.getKey().getType());
						}
					} catch (IOException e) {
						// We should not have not-supported files in the
						// library
						Instance.syserr(new IOException(
								"Cannot load file from library: "
										+ entry.getValue().getPath(), e));
					}
				}
			}
		}

		return null;
	}

	/**
	 * Import the {@link Story} at the given {@link URL} into the
	 * {@link Library}.
	 * 
	 * @param url
	 *            the {@link URL} to import
	 * 
	 * @return the imported {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Story imprt(URL url) throws IOException {
		BasicSupport support = BasicSupport.getSupport(url);
		if (support == null) {
			throw new IOException("URL not supported: " + url.toString());
		}

		getStories(); // refresh lastId
		Story story = support.process(url);
		story.getMeta().setLuid(String.format("%03d", (++lastId)));
		save(story);

		return story;
	}

	/**
	 * Export the {@link Story} to the given target in the given format.
	 * 
	 * @param luid
	 *            the {@link Story} ID
	 * @param type
	 *            the {@link OutputType} to transform it to
	 * @param target
	 *            the target to save to
	 * 
	 * @return the saved resource (the main saved {@link File})
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public File export(String luid, OutputType type, String target)
			throws IOException {
		BasicOutput out = BasicOutput.getOutput(type, true);
		if (out == null) {
			throw new IOException("Output type not supported: " + type);
		}

		return out.process(getStory(luid), target);
	}

	/**
	 * Save a story as-is to the {@link Library} -- the LUID <b>must</b> be
	 * correct.
	 * 
	 * @param story
	 *            the {@link Story} to save
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private void save(Story story) throws IOException {
		MetaData key = story.getMeta();

		getDir(key).mkdirs();
		if (!getDir(key).exists()) {
			throw new IOException("Cannot create library dir");
		}

		OutputType out;
		SupportType in;
		if (key != null && key.isImageDocument()) {
			in = SupportType.CBZ;
			out = OutputType.CBZ;
		} else {
			in = SupportType.INFO_TEXT;
			out = OutputType.INFO_TEXT;
		}
		BasicOutput it = BasicOutput.getOutput(out, true);
		File file = it.process(story, getFile(key).getPath());
		getStories().put(
				BasicSupport.getSupport(in).processMeta(file.toURI().toURL())
						.getMeta(), file);
	}

	/**
	 * The directory (full path) where the {@link Story} related to this
	 * {@link MetaData} should be located on disk.
	 * 
	 * @param key
	 *            the {@link Story} {@link MetaData}
	 * 
	 * @return the target directory
	 */
	private File getDir(MetaData key) {
		String source = key.getSource().replaceAll("[^a-zA-Z0-9._+-]", "_");
		return new File(baseDir, source);
	}

	/**
	 * The target (full path) where the {@link Story} related to this
	 * {@link MetaData} should be located on disk.
	 * 
	 * @param key
	 *            the {@link Story} {@link MetaData}
	 * 
	 * @return the target
	 */
	private File getFile(MetaData key) {
		String title = key.getTitle().replaceAll("[^a-zA-Z0-9._+-]", "_");
		return new File(getDir(key), key.getLuid() + "_" + title);
	}

	/**
	 * Return all the known stories in this {@link Library} object.
	 * 
	 * @return the stories
	 */
	private Map<MetaData, File> getStories() {
		if (stories.isEmpty()) {
			lastId = 0;
			String format = "."
					+ Instance.getConfig().getString(Config.IMAGE_FORMAT_COVER)
							.toLowerCase();
			for (File dir : baseDir.listFiles()) {
				if (dir.isDirectory()) {
					for (File file : dir.listFiles()) {
						try {
							String path = file.getPath().toLowerCase();
							if (!path.endsWith(".info")
									&& !path.endsWith(format)) {
								// TODO: export .info reading to a class and use
								// it here
								SupportType type = SupportType.INFO_TEXT;
								if (path.toLowerCase().endsWith(".cbz")) {
									type = SupportType.CBZ;
								}
								BasicSupport support = BasicSupport
										.getSupport(type);
								MetaData meta = support.processMeta(
										file.toURI().toURL()).getMeta();
								if (meta != null) {
									stories.put(meta, file);
									try {
										int id = Integer.parseInt(meta
												.getLuid());
										if (id > lastId) {
											lastId = id;
										}
									} catch (Exception e) {
										// not normal!!
										Instance.syserr(new IOException(
												"Cannot understand the LUID of "
														+ file.getPath() + ": "
														+ meta.getLuid(), e));
									}
								} else {
									// not normal!!
									Instance.syserr(new IOException(
											"Cannot get metadata for: "
													+ file.getPath()));
								}
							}
						} catch (IOException e) {
							// We should not have not-supported files in the
							// library
							Instance.syserr(new IOException(
									"Cannot load file from library: "
											+ file.getPath(), e));
						}
					}
				}
			}
		}

		return stories;
	}
}
