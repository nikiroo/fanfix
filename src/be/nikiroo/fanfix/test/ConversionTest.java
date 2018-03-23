package be.nikiroo.fanfix.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.Main;
import be.nikiroo.fanfix.output.BasicOutput;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.TempFiles;
import be.nikiroo.utils.TraceHandler;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

class ConversionTest extends TestLauncher {
	private TempFiles tempFiles;
	private File testFile;
	private File expectedDir;
	private File resultDir;
	private List<BasicOutput.OutputType> realTypes;
	private Map<String, List<String>> skipCompare;

	public ConversionTest(String[] args) {
		super("Conversion", args);

		// Special mode SYSOUT is not a file type (System.out)
		realTypes = new ArrayList<BasicOutput.OutputType>();
		for (BasicOutput.OutputType type : BasicOutput.OutputType.values()) {
			if (!BasicOutput.OutputType.SYSOUT.equals(type)) {
				realTypes.add(type);
			}
		}

		addTest(new TestCase("Read the test file") {
			@Override
			public void test() throws Exception {
				assertEquals("The test file \"" + testFile
						+ "\" cannot be found", true, testFile.exists());
			}
		});

		addTest(new TestCase("Assure directories exist") {
			@Override
			public void test() throws Exception {
				expectedDir.mkdirs();
				resultDir.mkdirs();
				assertEquals("The Expected directory \"" + expectedDir
						+ "\" cannot be created", true, expectedDir.exists());
				assertEquals("The Result directory \"" + resultDir
						+ "\" cannot be created", true, resultDir.exists());
			}
		});

		for (BasicOutput.OutputType type : realTypes) {
			addTest(getTestFor(type));
		}
	}

	@Override
	protected void start() throws Exception {
		testFile = new File("test/test.story");
		expectedDir = new File("test/expected/");
		resultDir = new File("test/result/");

		tempFiles = new TempFiles("Fanfix-ConversionTest");

		skipCompare = new HashMap<String, List<String>>();
		skipCompare.put("epb.ncx",
				Arrays.asList("		<meta name=\"dtb:uid\" content="));
		skipCompare.put("epb.opf", Arrays.asList("      <dc:subject>",
				"      <dc:identifier id=\"BookId\" opf:scheme=\"URI\">"));
		skipCompare.put(".info",
				Arrays.asList("CREATION_DATE=", "SUBJECT=", "URL=", "UUID="));
		skipCompare.put("URL", Arrays.asList("file:/"));
	}

	@Override
	protected void stop() throws Exception {
		tempFiles.close();
	}

	private TestCase getTestFor(final BasicOutput.OutputType type) {
		return new TestCase(type + " output mode") {
			@Override
			public void test() throws Exception {
				File target = generate(this, testFile, resultDir, type);
				target = new File(target.getAbsolutePath()
						+ type.getDefaultExtension(false));

				// Check conversion:
				compareFiles(this, expectedDir, resultDir, type, null);

				// LATEX not supported as input
				if (BasicOutput.OutputType.LATEX.equals(type)) {
					return;
				}

				// Cross-checks:
				for (BasicOutput.OutputType crossType : realTypes) {
					File crossDir = tempFiles.createTempDir("cross-result");
					generate(this, target, crossDir, crossType);
					compareFiles(this, resultDir, crossDir, crossType,
							crossType);
				}
			}
		};
	}

	private File generate(TestCase testCase, File testFile, File resultDir,
			BasicOutput.OutputType type) throws Exception {
		final List<String> errors = new ArrayList<String>();

		TraceHandler previousTraceHandler = Instance.getTraceHandler();
		Instance.setTraceHandler(new TraceHandler(true, true, 0) {
			@Override
			public void error(String message) {
				errors.add(message);
			}

			@Override
			public void error(Exception e) {
				error(" ");
				for (Throwable t = e; t != null; t = t.getCause()) {
					error(((t == e) ? "(" : "..caused by: (")
							+ t.getClass().getSimpleName() + ") "
							+ t.getMessage());
					for (StackTraceElement s : t.getStackTrace()) {
						error("\t" + s.toString());
					}
				}
			}
		});

		try {
			File target = new File(resultDir, type.toString());
			int code = Main.convert(testFile.getAbsolutePath(),
					type.toString(), target.getAbsolutePath(), false, null);

			String error = "";
			for (String err : errors) {
				if (!error.isEmpty())
					error += "\n";
				error += err;
			}
			testCase.assertEquals("The conversion returned an error message: "
					+ error, 0, errors.size());
			if (code != 0) {
				testCase.fail("The conversion failed with return code: " + code);
			}

			return target;
		} finally {
			Instance.setTraceHandler(previousTraceHandler);
		}
	}

	private void compareFiles(TestCase testCase, File expectedDir,
			File resultDir, final BasicOutput.OutputType typeToCompare,
			final BasicOutput.OutputType sourceType) throws Exception {

		FilenameFilter filter = null;
		if (typeToCompare != null) {
			filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith(typeToCompare.toString());
				}
			};
		}

		List<String> resultFiles = Arrays.asList(resultDir.list(filter));
		resultFiles.sort(null);
		List<String> expectedFiles = Arrays.asList(expectedDir.list(filter));
		expectedFiles.sort(null);

		testCase.assertEquals("The resulting file names are not expected",
				expectedFiles, resultFiles);

		for (int i = 0; i < resultFiles.size(); i++) {
			File expected = new File(expectedDir, expectedFiles.get(i));
			File result = new File(resultDir, resultFiles.get(i));

			testCase.assertEquals(
					"Type mismatch: expected a "
							+ (expected.isDirectory() ? "directory" : "file")
							+ ", received a "
							+ (result.isDirectory() ? "directory" : "file"),
					expected.isDirectory(), result.isDirectory());

			if (expected.isDirectory()) {
				compareFiles(testCase, expected, result, null, sourceType);
				continue;
			}

			if (expected.getName().endsWith(".cbz")
					|| expected.getName().endsWith(".epub")) {
				File tmpExpected = tempFiles.createTempDir(expected.getName()
						+ "[zip-content]");
				File tmpResult = tempFiles.createTempDir(result.getName()
						+ "[zip-content]");
				unzip(expected, tmpExpected);
				unzip(result, tmpResult);
				compareFiles(testCase, tmpExpected, tmpResult, null, sourceType);
			} else {
				List<String> expectedLines = Arrays.asList(IOUtils
						.readSmallFile(expected).split("\n"));
				List<String> resultLines = Arrays.asList(IOUtils.readSmallFile(
						result).split("\n"));

				String name = expected.getAbsolutePath();
				if (name.startsWith(expectedDir.getAbsolutePath())) {
					name = expectedDir.getName()
							+ name.substring(expectedDir.getAbsolutePath()
									.length());
				}
				for (int j = 0; j < expectedLines.size(); j++) {
					String expectedLine = expectedLines.get(j);
					String resultLine = resultLines.get(j);

					boolean skip = false;
					for (Entry<String, List<String>> skipThose : skipCompare
							.entrySet()) {
						for (String skipStart : skipThose.getValue()) {
							if (name.endsWith(skipThose.getKey())
									&& expectedLine.startsWith(skipStart)
									&& resultLine.startsWith(skipStart)) {
								skip = true;
							}
						}
					}

					if (skip) {
						continue;
					}

					testCase.assertEquals("Line " + (j + 1) + " (" + sourceType
							+ ") is not the same in file " + name,
							expectedLine, resultLine);
				}
			}
		}
	}

	// TODO: remove and use IOUtils when updated
	private static void unzip(File zipFile, File targetDirectory)
			throws IOException {
		if (targetDirectory.exists() && targetDirectory.isFile()) {
			throw new IOException("Cannot unzip " + zipFile + " into "
					+ targetDirectory + ": it is not a directory");
		}

		targetDirectory.mkdir();
		if (!targetDirectory.exists()) {
			throw new IOException("Cannot create target directory "
					+ targetDirectory);
		}

		FileInputStream in = new FileInputStream(zipFile);
		try {
			ZipInputStream zipStream = new ZipInputStream(in);
			try {
				for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream
						.getNextEntry()) {
					File file = new File(targetDirectory, entry.getName());
					if (entry.isDirectory()) {
						file.mkdirs();
					} else {
						IOUtils.write(zipStream, file);
					}
				}
			} finally {
				zipStream.close();
			}
		} finally {
			in.close();
		}
	}
}
