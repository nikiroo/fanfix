package be.nikiroo.fanfix;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.output.InfoCover;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;
import be.nikiroo.fanfix.supported.InfoReader;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.Progress;

/**
 * Manage a library of Stories: import, export, list.
 * <p>
 * Each {@link Story} object will be associated with a (local to the library)
 * unique ID, the LUID, which will be used to identify the {@link Story}.
 * <p>
 * Most of the {@link Library} functions work on either the LUID or a partial
 * (cover not included) {@link MetaData} object.
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
	 * Refresh the {@link Library}, that is, make sure all stories are loaded.
	 * 
	 * @param pg
	 *            the optional progress reporter
	 */
	public void refresh(Progress pg) {
		getStories(pg);
	}

	/**
	 * List all the known types (sources) of stories.
	 * 
	 * @return the types
	 */
	public synchronized List<String> getTypes() {
		List<String> list = new ArrayList<String>();
		for (Entry<MetaData, File> entry : getStories(null).entrySet()) {
			String storyType = entry.getKey().getSource();
			if (!list.contains(storyType)) {
				list.add(storyType);
			}
		}

		Collections.sort(list);
		return list;
	}

	/**
	 * List all the known authors of stories.
	 * 
	 * @return the authors
	 */
	public synchronized List<String> getAuthors() {
		List<String> list = new ArrayList<String>();
		for (Entry<MetaData, File> entry : getStories(null).entrySet()) {
			String storyAuthor = entry.getKey().getAuthor();
			if (!list.contains(storyAuthor)) {
				list.add(storyAuthor);
			}
		}

		Collections.sort(list);
		return list;
	}

	/**
	 * List all the stories of the given author in the {@link Library}, or all
	 * the stories if NULL is passed as an author.
	 * <p>
	 * Cover images not included.
	 * 
	 * @param author
	 *            the author of the stories to retrieve, or NULL for all
	 * 
	 * @return the stories
	 */
	public synchronized List<MetaData> getListByAuthor(String author) {
		List<MetaData> list = new ArrayList<MetaData>();
		for (Entry<MetaData, File> entry : getStories(null).entrySet()) {
			String storyAuthor = entry.getKey().getAuthor();
			if (author == null || author.equalsIgnoreCase(storyAuthor)) {
				list.add(entry.getKey());
			}
		}

		Collections.sort(list);
		return list;
	}

	/**
	 * List all the stories of the given source type in the {@link Library}, or
	 * all the stories if NULL is passed as a type.
	 * <p>
	 * Cover images not included.
	 * 
	 * @param type
	 *            the type of story to retrieve, or NULL for all
	 * 
	 * @return the stories
	 */
	public synchronized List<MetaData> getListByType(String type) {
		if (type != null) {
			// convert the type to dir name
			type = getDir(type).getName();
		}

		List<MetaData> list = new ArrayList<MetaData>();
		for (Entry<MetaData, File> entry : getStories(null).entrySet()) {
			String storyType = entry.getValue().getParentFile().getName();
			if (type == null || type.equalsIgnoreCase(storyType)) {
				list.add(entry.getKey());
			}
		}

		Collections.sort(list);
		return list;
	}

	/**
	 * Retrieve a {@link File} corresponding to the given {@link Story}, cover
	 * image not included.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * 
	 * @return the corresponding {@link Story}
	 */
	public synchronized MetaData getInfo(String luid) {
		if (luid != null) {
			for (Entry<MetaData, File> entry : getStories(null).entrySet()) {
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
			for (Entry<MetaData, File> entry : getStories(null).entrySet()) {
				if (luid.equals(entry.getKey().getLuid())) {
					return entry.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * Return the cover image associated to this story.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * 
	 * @return the cover image
	 */
	public synchronized BufferedImage getCover(String luid) {
		MetaData meta = getInfo(luid);
		if (meta != null) {
			try {
				File infoFile = new File(getFile(meta).getPath() + ".info");
				meta = readMeta(infoFile, true).getKey();
				return meta.getCover();
			} catch (IOException e) {
				Instance.syserr(e);
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
			for (Entry<MetaData, File> entry : getStories(null).entrySet()) {
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
			getStories(null); // refresh lastId if needed
			key.setLuid(String.format("%03d", (++lastId)));
		} else {
			key.setLuid(luid);
		}

		getDir(key.getSource()).mkdirs();
		if (!getDir(key.getSource()).exists()) {
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

		List<File> files = getFiles(luid);
		if (!files.isEmpty()) {
			for (File file : files) {
				IOUtils.deltree(file);
			}

			ok = true;

			// clear cache
			stories.clear();
		}

		return ok;
	}

	/**
	 * Change the type (source) of the given {@link Story}.
	 * 
	 * @param luid
	 *            the {@link Story} LUID
	 * @param newType
	 *            the new type
	 * 
	 * @return TRUE if the {@link Story} was found
	 */
	public synchronized boolean changeType(String luid, String newType) {
		MetaData meta = getInfo(luid);
		if (meta != null) {
			meta.setSource(newType);
			File newDir = getDir(meta.getSource());
			if (!newDir.exists()) {
				newDir.mkdir();
			}

			List<File> files = getFiles(luid);
			for (File file : files) {
				if (file.getName().endsWith(".info")) {
					try {
						String name = file.getName().replaceFirst("\\.info$",
								"");
						InfoCover.writeInfo(newDir, name, meta);
						file.delete();
					} catch (IOException e) {
						Instance.syserr(e);
					}
				} else {
					file.renameTo(new File(newDir, file.getName()));
				}
			}

			// clear cache
			stories.clear();

			return true;
		}

		return false;
	}

	/**
	 * Return the list of files/dirs on disk for this {@link Story}.
	 * <p>
	 * If the {@link Story} is not found, and empty list is returned.
	 * 
	 * @param luid
	 *            the {@link Story} LUID
	 * 
	 * @return the list of {@link File}s
	 */
	private List<File> getFiles(String luid) {
		List<File> files = new ArrayList<File>();

		MetaData meta = getInfo(luid);
		File file = getStories(null).get(meta);

		if (file != null) {
			files.add(file);

			String readerExt = getOutputType(meta).getDefaultExtension(true);
			String fileExt = getOutputType(meta).getDefaultExtension(false);

			String path = file.getAbsolutePath();
			if (readerExt != null && !readerExt.equals(fileExt)) {
				path = path.substring(0, path.length() - readerExt.length())
						+ fileExt;
				file = new File(path);

				if (file.exists()) {
					files.add(file);
				}
			}

			File infoFile = new File(path + ".info");
			if (!infoFile.exists()) {
				infoFile = new File(path.substring(0,
						path.length() - fileExt.length())
						+ ".info");
			}

			if (infoFile.exists()) {
				files.add(infoFile);
			}

			String coverExt = "."
					+ Instance.getConfig().getString(Config.IMAGE_FORMAT_COVER);
			File coverFile = new File(path + coverExt);
			if (!coverFile.exists()) {
				coverFile = new File(path.substring(0,
						path.length() - fileExt.length())
						+ coverExt);
			}

			if (coverFile.exists()) {
				files.add(coverFile);
			}
		}

		return files;
	}

	/**
	 * The directory (full path) where the {@link Story} related to this
	 * {@link MetaData} should be located on disk.
	 * 
	 * @param type
	 *            the type (source)
	 * 
	 * @return the target directory
	 */
	private File getDir(String type) {
		String source = type.replaceAll("[^a-zA-Z0-9._+-]", "_");
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
		String title = key.getTitle();
		if (title == null) {
			title = "";
		}
		title = title.replaceAll("[^a-zA-Z0-9._+-]", "_");
		return new File(getDir(key.getSource()), key.getLuid() + "_" + title);
	}

	/**
	 * Return all the known stories in this {@link Library} object.
	 * 
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the stories
	 */
	private synchronized Map<MetaData, File> getStories(Progress pg) {
		if (pg == null) {
			pg = new Progress();
		} else {
			pg.setMinMax(0, 100);
		}

		if (stories.isEmpty()) {
			lastId = 0;

			File[] dirs = baseDir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return file != null && file.isDirectory();
				}
			});

			Progress pgDirs = new Progress(0, 100 * dirs.length);
			pg.addProgress(pgDirs, 100);

			for (File dir : dirs) {
				File[] files = dir.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return file != null
								&& file.getPath().toLowerCase()
										.endsWith(".info");
					}
				});

				Progress pgFiles = new Progress(0, files.length);
				pgDirs.addProgress(pgFiles, 100);
				pgDirs.setName("Loading from: " + dir.getName());

				for (File file : files) {
					pgFiles.setName(file.getName());
					try {
						Entry<MetaData, File> entry = readMeta(file, false);
						try {
							int id = Integer.parseInt(entry.getKey().getLuid());
							if (id > lastId) {
								lastId = id;
							}

							stories.put(entry.getKey(), entry.getValue());
						} catch (Exception e) {
							// not normal!!
							throw new IOException(
									"Cannot understand the LUID of "
											+ file.getPath() + ": "
											+ entry.getKey().getLuid(), e);
						}
					} catch (IOException e) {
						// We should not have not-supported files in the
						// library
						Instance.syserr(new IOException(
								"Cannot load file from library: "
										+ file.getPath(), e));
					}
					pgFiles.add(1);
				}

				pgFiles.setName(null);
			}

			pgDirs.setName("Loading directories");
		}

		return stories;
	}

	private Entry<MetaData, File> readMeta(File infoFile, boolean withCover)
			throws IOException {

		final MetaData meta = InfoReader.readMeta(infoFile, withCover);

		// Replace .info with whatever is needed:
		String path = infoFile.getPath();
		path = path.substring(0, path.length() - ".info".length());

		String newExt = getOutputType(meta).getDefaultExtension(true);

		File targetFile = new File(path + newExt);

		final File ffile = targetFile;
		return new Entry<MetaData, File>() {
			public File setValue(File value) {
				return null;
			}

			public File getValue() {
				return ffile;
			}

			public MetaData getKey() {
				return meta;
			}
		};
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
