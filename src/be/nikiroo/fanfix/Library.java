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
import be.nikiroo.fanfix.supported.InfoReader;
import be.nikiroo.utils.Progress;

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
	private OutputType text;
	private OutputType image;

	/**
	 * Create a new {@link Library} with the given backend directory.
	 * 
	 * @param dir
	 *            the directory where to find the {@link Story} objects
	 * @param text
	 *            the {@link OutputType} to save the text-focused stories into
	 * @param image
	 *            the {@link OutputType} to save the images-focused stories into
	 */
	public Library(File dir, OutputType text, OutputType image) {
		this.baseDir = dir;
		this.stories = new HashMap<MetaData, File>();
		this.lastId = 0;
		this.text = text;
		this.image = image;

		dir.mkdirs();
	}

	/**
	 * List all the known types of stories.
	 * 
	 * @return the types
	 */
	public synchronized List<String> getTypes() {
		List<String> list = new ArrayList<String>();
		for (Entry<MetaData, File> entry : getStories().entrySet()) {
			String storyType = entry.getValue().getParentFile().getName();
			if (!list.contains(storyType)) {
				list.add(storyType);
			}
		}

		return list;
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
	public synchronized List<MetaData> getList(String type) {
		List<MetaData> list = new ArrayList<MetaData>();
		for (Entry<MetaData, File> entry : getStories().entrySet()) {
			String storyType = entry.getValue().getParentFile().getName();
			if (type == null || type.equalsIgnoreCase(storyType)) {
				list.add(entry.getKey());
			}
		}

		return list;
	}

	/**
	 * Retrieve a {@link File} corresponding to the given {@link Story}.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * 
	 * @return the corresponding {@link Story}
	 */
	public synchronized MetaData getInfo(String luid) {
		if (luid != null) {
			for (Entry<MetaData, File> entry : getStories().entrySet()) {
				if (luid.equals(entry.getKey().getLuid())) {
					return entry.getKey();
				}
			}
		}

		return null;
	}

	/**
	 * Retrieve a {@link File} corresponding to the given {@link Story}.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * 
	 * @return the corresponding {@link Story}
	 */
	public synchronized File getFile(String luid) {
		if (luid != null) {
			for (Entry<MetaData, File> entry : getStories().entrySet()) {
				if (luid.equals(entry.getKey().getLuid())) {
					return entry.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * Retrieve a specific {@link Story}.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the corresponding {@link Story} or NULL if not found
	 */
	public synchronized Story getStory(String luid, Progress pg) {
		if (luid != null) {
			for (Entry<MetaData, File> entry : getStories().entrySet()) {
				if (luid.equals(entry.getKey().getLuid())) {
					try {
						SupportType type = SupportType.valueOfAllOkUC(entry
								.getKey().getType());
						URL url = entry.getValue().toURI().toURL();
						if (type != null) {
							return BasicSupport.getSupport(type).process(url,
									pg);
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

		if (pg != null) {
			pg.setMinMax(0, 1);
			pg.setProgress(1);
		}

		return null;
	}

	/**
	 * Import the {@link Story} at the given {@link URL} into the
	 * {@link Library}.
	 * 
	 * @param url
	 *            the {@link URL} to import
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the imported {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Story imprt(URL url, Progress pg) throws IOException {
		BasicSupport support = BasicSupport.getSupport(url);
		if (support == null) {
			throw new IOException("URL not supported: " + url.toString());
		}

		return save(support.process(url, pg), null);
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
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the saved resource (the main saved {@link File})
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public File export(String luid, OutputType type, String target, Progress pg)
			throws IOException {
		Progress pgGetStory = new Progress();
		Progress pgOut = new Progress();
		if (pg != null) {
			pg.setMax(2);
			pg.addProgress(pgGetStory, 1);
			pg.addProgress(pgOut, 1);
		}

		BasicOutput out = BasicOutput.getOutput(type, true);
		if (out == null) {
			throw new IOException("Output type not supported: " + type);
		}

		Story story = getStory(luid, pgGetStory);
		if (story == null) {
			throw new IOException("Cannot find story to export: " + luid);
		}

		return out.process(story, target, pgOut);
	}

	/**
	 * Save a {@link Story} to the {@link Library}.
	 * 
	 * @param story
	 *            the {@link Story} to save
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the same {@link Story}, whose LUID may have changed
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Story save(Story story, Progress pg) throws IOException {
		return save(story, null, pg);
	}

	/**
	 * Save a {@link Story} to the {@link Library} -- the LUID <b>must</b> be
	 * correct, or NULL to get the next free one.
	 * 
	 * @param story
	 *            the {@link Story} to save
	 * @param luid
	 *            the <b>correct</b> LUID or NULL to get the next free one
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the same {@link Story}, whose LUID may have changed
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public synchronized Story save(Story story, String luid, Progress pg)
			throws IOException {
		// Do not change the original metadata, but change the original story
		MetaData key = story.getMeta().clone();
		story.setMeta(key);

		if (luid == null || luid.isEmpty()) {
			getStories(); // refresh lastId if needed
			key.setLuid(String.format("%03d", (++lastId)));
		} else {
			key.setLuid(luid);
		}

		getDir(key).mkdirs();
		if (!getDir(key).exists()) {
			throw new IOException("Cannot create library dir");
		}

		OutputType out;
		if (key != null && key.isImageDocument()) {
			out = image;
		} else {
			out = text;
		}

		BasicOutput it = BasicOutput.getOutput(out, true);
		it.process(story, getFile(key).getPath(), pg);

		// empty cache
		stories.clear();

		return story;
	}

	/**
	 * Delete the given {@link Story} from this {@link Library}.
	 * 
	 * @param luid
	 *            the LUID of the target {@link Story}
	 * 
	 * @return TRUE if it was deleted
	 */
	public synchronized boolean delete(String luid) {
		boolean ok = false;

		MetaData meta = getInfo(luid);
		File file = getStories().get(meta);

		if (file != null) {
			if (file.delete()) {
				String newExt = getOutputType(meta).getDefaultExtension(false);

				String path = file.getAbsolutePath();
				File infoFile = new File(path + ".info");
				if (!infoFile.exists()) {
					infoFile = new File(path.substring(0, path.length()
							- newExt.length())
							+ ".info");
				}
				infoFile.delete();

				String coverExt = "."
						+ Instance.getConfig().getString(
								Config.IMAGE_FORMAT_COVER);
				File coverFile = new File(path + coverExt);
				if (!coverFile.exists()) {
					coverFile = new File(path.substring(0, path.length()
							- newExt.length()));
				}
				coverFile.delete();

				ok = true;
			}

			// clear cache
			stories.clear();
		}

		return ok;
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
	private synchronized Map<MetaData, File> getStories() {
		if (stories.isEmpty()) {
			lastId = 0;

			String ext = ".info";
			for (File dir : baseDir.listFiles()) {
				if (dir.isDirectory()) {
					for (File file : dir.listFiles()) {
						try {
							if (file.getPath().toLowerCase().endsWith(ext)) {
								MetaData meta = InfoReader.readMeta(file);
								try {
									int id = Integer.parseInt(meta.getLuid());
									if (id > lastId) {
										lastId = id;
									}

									// Replace .info with whatever is needed:
									String path = file.getPath();
									path = path.substring(0, path.length()
											- ext.length());

									String newExt = getOutputType(meta)
											.getDefaultExtension(true);

									file = new File(path + newExt);
									//

									stories.put(meta, file);

								} catch (Exception e) {
									// not normal!!
									Instance.syserr(new IOException(
											"Cannot understand the LUID of "
													+ file.getPath() + ": "
													+ meta.getLuid(), e));
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

	/**
	 * Return the {@link OutputType} for this {@link Story}.
	 * 
	 * @param meta
	 *            the {@link Story} {@link MetaData}
	 * 
	 * @return the type
	 */
	private OutputType getOutputType(MetaData meta) {
		if (meta != null && meta.isImageDocument()) {
			return image;
		} else {
			return text;
		}
	}
}
