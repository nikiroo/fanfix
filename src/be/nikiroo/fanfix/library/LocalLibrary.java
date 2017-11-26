package be.nikiroo.fanfix.library;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.output.InfoCover;
import be.nikiroo.fanfix.supported.InfoReader;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.ImageUtils;
import be.nikiroo.utils.MarkableFileInputStream;
import be.nikiroo.utils.Progress;

/**
 * This {@link BasicLibrary} will store the stories locally on disk.
 * 
 * @author niki
 */
public class LocalLibrary extends BasicLibrary {
	private int lastId;
	private Map<MetaData, File[]> stories; // Files: [ infoFile, TargetFile ]
	private Map<String, BufferedImage> sourceCovers;

	private File baseDir;
	private OutputType text;
	private OutputType image;

	/**
	 * Create a new {@link LocalLibrary} with the given back-end directory.
	 * 
	 * @param baseDir
	 *            the directory where to find the {@link Story} objects
	 */
	public LocalLibrary(File baseDir) {
		this(baseDir, Instance.getConfig().getString(
				Config.NON_IMAGES_DOCUMENT_TYPE), Instance.getConfig()
				.getString(Config.IMAGES_DOCUMENT_TYPE), false);
	}

	/**
	 * Create a new {@link LocalLibrary} with the given back-end directory.
	 * 
	 * @param baseDir
	 *            the directory where to find the {@link Story} objects
	 * @param text
	 *            the {@link OutputType} to use for non-image documents
	 * @param image
	 *            the {@link OutputType} to use for image documents
	 * @param defaultIsHtml
	 *            if the given text or image is invalid, use HTML by default (if
	 *            not, it will be INFO_TEXT/CBZ by default)
	 */
	public LocalLibrary(File baseDir, String text, String image,
			boolean defaultIsHtml) {
		this(baseDir, OutputType.valueOfAllOkUC(text,
				defaultIsHtml ? OutputType.HTML : OutputType.INFO_TEXT),
				OutputType.valueOfAllOkUC(image,
						defaultIsHtml ? OutputType.HTML : OutputType.CBZ));
	}

	/**
	 * Create a new {@link LocalLibrary} with the given back-end directory.
	 * 
	 * @param baseDir
	 *            the directory where to find the {@link Story} objects
	 * @param text
	 *            the {@link OutputType} to use for non-image documents
	 * @param image
	 *            the {@link OutputType} to use for image documents
	 */
	public LocalLibrary(File baseDir, OutputType text, OutputType image) {
		this.baseDir = baseDir;
		this.text = text;
		this.image = image;

		this.lastId = 0;
		this.stories = null;
		this.sourceCovers = new HashMap<String, BufferedImage>();

		baseDir.mkdirs();
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) {
		return new ArrayList<MetaData>(getStories(pg).keySet());
	}

	@Override
	public File getFile(String luid, Progress pg) {
		File[] files = getStories(pg).get(getInfo(luid));
		if (files != null) {
			return files[1];
		}

		return null;
	}

	@Override
	public BufferedImage getCover(String luid) {
		MetaData meta = getInfo(luid);
		if (meta != null) {
			File[] files = getStories(null).get(meta);
			if (files != null) {
				File infoFile = files[0];

				try {
					meta = InfoReader.readMeta(infoFile, true);
					return meta.getCover();
				} catch (IOException e) {
					Instance.syserr(e);
				}
			}
		}

		return null;
	}

	@Override
	protected void clearCache() {
		stories = null;
		sourceCovers = new HashMap<String, BufferedImage>();
	}

	@Override
	protected synchronized int getNextId() {
		getStories(null); // make sure lastId is set
		return ++lastId;
	}

	@Override
	protected void doDelete(String luid) throws IOException {
		for (File file : getRelatedFiles(luid)) {
			// TODO: throw an IOException if we cannot delete the files?
			IOUtils.deltree(file);
		}
	}

	@Override
	protected Story doSave(Story story, Progress pg) throws IOException {
		MetaData meta = story.getMeta();

		File expectedTarget = getExpectedFile(meta);
		expectedTarget.getParentFile().mkdirs();

		BasicOutput it = BasicOutput.getOutput(getOutputType(meta), true);
		it.process(story, expectedTarget.getPath(), pg);

		return story;
	}

	@Override
	protected synchronized void saveMeta(MetaData meta, Progress pg)
			throws IOException {
		File newDir = getExpectedDir(meta.getSource());
		if (!newDir.exists()) {
			newDir.mkdir();
		}

		List<File> relatedFiles = getRelatedFiles(meta.getLuid());
		for (File relatedFile : relatedFiles) {
			// TODO: this is not safe at all.
			// We should copy all the files THEN delete them
			// Maybe also adding some rollback cleanup if possible
			if (relatedFile.getName().endsWith(".info")) {
				try {
					String name = relatedFile.getName().replaceFirst(
							"\\.info$", "");
					InfoCover.writeInfo(newDir, name, meta);
					relatedFile.delete();
				} catch (IOException e) {
					Instance.syserr(e);
				}
			} else {
				relatedFile.renameTo(new File(newDir, relatedFile.getName()));
			}
		}

		clearCache();
	}

	@Override
	public BufferedImage getSourceCover(String source) {
		if (!sourceCovers.containsKey(source)) {
			sourceCovers.put(source, super.getSourceCover(source));
		}

		return sourceCovers.get(source);
	}

	@Override
	public void setSourceCover(String source, String luid) {
		sourceCovers.put(source, getCover(luid));
		File cover = new File(getExpectedDir(source), ".cover.png");
		try {
			ImageIO.write(sourceCovers.get(source), "png", cover);
		} catch (IOException e) {
			Instance.syserr(e);
			sourceCovers.remove(source);
		}
	}

	@Override
	public void imprt(BasicLibrary other, String luid, Progress pg)
			throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		// Check if we can simply copy the files instead of the whole process
		if (other instanceof LocalLibrary) {
			LocalLibrary otherLocalLibrary = (LocalLibrary) other;

			MetaData meta = otherLocalLibrary.getInfo(luid);
			String expectedType = ""
					+ (meta != null && meta.isImageDocument() ? image : text);
			if (meta != null && meta.getType().equals(expectedType)) {
				File from = otherLocalLibrary.getExpectedDir(meta.getSource());
				File to = this.getExpectedDir(meta.getSource());
				List<File> sources = otherLocalLibrary.getRelatedFiles(luid);
				if (!sources.isEmpty()) {
					pg.setMinMax(0, sources.size());
				}

				for (File source : sources) {
					File target = new File(source.getAbsolutePath().replace(
							from.getAbsolutePath(), to.getAbsolutePath()));
					if (!source.equals(target)) {
						target.getParentFile().mkdirs();
						InputStream in = null;
						try {
							in = new FileInputStream(source);
							IOUtils.write(in, target);
						} catch (IOException e) {
							if (in != null) {
								try {
									in.close();
								} catch (Exception ee) {
								}
							}

							pg.done();
							throw e;
						}
					}

					pg.add(1);
				}

				clearCache();
				pg.done();
				return;
			}
		}

		super.imprt(other, luid, pg);

		clearCache();
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
		}

		return text;
	}

	/**
	 * Get the target {@link File} related to the given <tt>.info</tt>
	 * {@link File} and {@link MetaData}.
	 * 
	 * @param meta
	 *            the meta
	 * @param infoFile
	 *            the <tt>.info</tt> {@link File}
	 * 
	 * @return the target {@link File}
	 */
	private File getTargetFile(MetaData meta, File infoFile) {
		// Replace .info with whatever is needed:
		String path = infoFile.getPath();
		path = path.substring(0, path.length() - ".info".length());
		String newExt = getOutputType(meta).getDefaultExtension(true);

		return new File(path + newExt);
	}

	/**
	 * The target (full path) where the {@link Story} related to this
	 * {@link MetaData} should be located on disk for a new {@link Story}.
	 * 
	 * @param key
	 *            the {@link Story} {@link MetaData}
	 * 
	 * @return the target
	 */
	private File getExpectedFile(MetaData key) {
		String title = key.getTitle();
		if (title == null) {
			title = "";
		}
		title = title.replaceAll("[^a-zA-Z0-9._+-]", "_");
		return new File(getExpectedDir(key.getSource()), key.getLuid() + "_"
				+ title);
	}

	/**
	 * The directory (full path) where the new {@link Story} related to this
	 * {@link MetaData} should be located on disk.
	 * 
	 * @param type
	 *            the type (source)
	 * 
	 * @return the target directory
	 */
	private File getExpectedDir(String type) {
		String source = type.replaceAll("[^a-zA-Z0-9._+-]", "_");
		return new File(baseDir, source);
	}

	/**
	 * Return the list of files/directories on disk for this {@link Story}.
	 * <p>
	 * If the {@link Story} is not found, and empty list is returned.
	 * 
	 * @param luid
	 *            the {@link Story} LUID
	 * 
	 * @return the list of {@link File}s
	 * 
	 * @throws IOException
	 *             if the {@link Story} was not found
	 */
	private List<File> getRelatedFiles(String luid) throws IOException {
		List<File> files = new ArrayList<File>();

		MetaData meta = getInfo(luid);
		if (meta == null) {
			throw new IOException("Story not found: " + luid);
		}

		File infoFile = getStories(null).get(meta)[0];
		File targetFile = getStories(null).get(meta)[1];

		files.add(infoFile);
		files.add(targetFile);

		String readerExt = getOutputType(meta).getDefaultExtension(true);
		String fileExt = getOutputType(meta).getDefaultExtension(false);

		String path = targetFile.getAbsolutePath();
		if (readerExt != null && !readerExt.equals(fileExt)) {
			path = path.substring(0, path.length() - readerExt.length())
					+ fileExt;
			File relatedFile = new File(path);

			if (relatedFile.exists()) {
				files.add(relatedFile);
			}
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

		return files;
	}

	/**
	 * Fill the list of stories by reading the content of the local directory
	 * {@link LocalLibrary#baseDir}.
	 * <p>
	 * Will use a cached list when possible (see
	 * {@link BasicLibrary#clearCache()}).
	 * 
	 * @param pg
	 *            the optional {@link Progress}
	 * 
	 * @return the list of stories
	 */
	private synchronized Map<MetaData, File[]> getStories(Progress pg) {
		if (pg == null) {
			pg = new Progress();
		} else {
			pg.setMinMax(0, 100);
		}

		if (stories == null) {
			stories = new HashMap<MetaData, File[]>();

			lastId = 0;

			File[] dirs = baseDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file != null && file.isDirectory();
				}
			});

			Progress pgDirs = new Progress(0, 100 * dirs.length);
			pg.addProgress(pgDirs, 100);

			for (File dir : dirs) {
				File[] infoFiles = dir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						return file != null
								&& file.getPath().toLowerCase()
										.endsWith(".info");
					}
				});

				Progress pgFiles = new Progress(0, infoFiles.length);
				pgDirs.addProgress(pgFiles, 100);
				pgDirs.setName("Loading from: " + dir.getName());

				String source = null;
				for (File infoFile : infoFiles) {
					pgFiles.setName(infoFile.getName());
					try {
						MetaData meta = InfoReader.readMeta(infoFile, false);
						source = meta.getSource();
						try {
							int id = Integer.parseInt(meta.getLuid());
							if (id > lastId) {
								lastId = id;
							}

							stories.put(meta, new File[] { infoFile,
									getTargetFile(meta, infoFile) });
						} catch (Exception e) {
							// not normal!!
							throw new IOException(
									"Cannot understand the LUID of "
											+ infoFile
											+ ": "
											+ (meta == null ? "[meta is NULL]"
													: meta.getLuid()), e);
						}
					} catch (IOException e) {
						// We should not have not-supported files in the
						// library
						Instance.syserr(new IOException(
								"Cannot load file from library: " + infoFile, e));
					}
					pgFiles.add(1);
				}

				File cover = new File(dir, ".cover.png");
				if (cover.exists()) {
					try {
						InputStream in = new MarkableFileInputStream(
								new FileInputStream(cover));
						try {
							sourceCovers.put(source, ImageUtils.fromStream(in));
						} finally {
							in.close();
						}
					} catch (IOException e) {
						Instance.syserr(e);
					}
				}

				pgFiles.setName(null);
			}

			pgDirs.setName("Loading directories");
		}

		return stories;
	}
}
