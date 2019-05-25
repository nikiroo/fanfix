# nikiroo-utils

## Version WIP

- new: server: count the bytes we rec/send
- new: CryptUtils
- new: stream classes
- new: Bundles can now also set Boolean, Integer... and not just get them
- new: Bundles get/setList()
- new: Bundles can now use the default values provided by the Meta
- fix: IOUtils.readSmallStream and \n at the end
- fix: Base64 implementation changed, no strange errors anymore
- change: StringUtils.unzip64(String) now returns a byte[] (StringUtils.unzip64s(String) can be used instead)
- change: be.nikiroo.utils.markableFileInputStream moved to be.nikiroo.utils.streams (old class still present in @Deprecated state for now)
- change: TestLauncher is now "silent" by default (no exception details, see setDetails(true))
- runtime: serial: SSL -> CryptUtils (both are **runtime** incompatible and CryptUtils is slower)
- runtime: break **runtime** compat with package utils.serial (Objects) -- ServerString should still be compatible (if not SSL obviously)

## Version 4.7.3

- fix: Downloader: POST and 302 redirects

## Version 4.7.2

- fix: Downloader issue with caching (input was spent when returned the first time)
- fix: IOUtil.forceResetableStream

## Version 4.7.1

- new: can now select the TempFiles root
- new: can now select the Image temporary root
- fix: Cache now offer some tracing when cleaning the cache
- fix: TemporaryFiles.close() was not indempotent (and was called in finalize!)

## Version 4.7.0

- tracer: traces on stderr by default
- new: downloader one more public method
- fix: downloader use original URL for cache ID

## Version 4.6.2

- fix: formatNumber/toNumber and decimals

## Version 4.6.0

- new: proxy
- new: StringUtils formatNumber and toNumber
- fix: UI Desc (NPE)

## Version 4.5.2

- Serial: fix b64/not b64 error
- Serial: perf improvement
- Base64: perf improvement
- new: Proxy selector

## Version 4.5.1

- Progress: fix deadlock in rare cases

## Version 4.5.0

- Base64: allow access to streams
- Deprecated: do not use our on deprecated functions
- Serial: fix ZIP/noZIP error

## Version 4.4.5

- Base64: allow access to not-zipped Base64 utilities
- Justify text: better handling of full text lines
- jDoc: improve
- IOUtils: new convenience method for reading a File into bytes

## Version 4.4.4

- Java 1.6: fix bad dependency so it can compiles on 1.6 again
- TempFilesTest: fix test
- Serial: fix for some constructors
- Serial: better default choice for ZIP/noZIP content

## Version 4.4.3

- Test assertions: fix files/dir content comparison code

## Version 4.4.2

- Test assertions: can now compare files/dir content

## Version 4.4.1

- Image: fix undocumented exception on save images
- TempFiles: crash early on error

## Version 4.4.0

- Text justification: now supports bullet lists and HR lines
- Text justification: fix a bug with dashes (-) and a crash
- Image to text converion fixes
- Serial: now supports anonymous inner classes
- Test: now allow an Exception argument to the "fail(..)" command
- Downloader: add an optional cache
- Cache: auto-clean when saving
- Bridge: fix a NPE when tracing
- New: justify, img2aa and bridge tools (see package Main)


## Version 4.3.0

- New: IOUtils.Unzip()
- TestCase: better message for lists comparisons

## Version 4.2.1

- Fix small bug in Downloader

## Version 4.2.0

- New getLanguage() in TransBundle

## Version 4.1.0

- New TempFiles (Image.java now uses it instead of memory)
- Auto cache cleaning + better errors in ImageUtilsAndroid
- New String justification options

## Version 4.0.1

- Android compatibility (see configure.sh --android=yes)

## Version 4.0.0

- Deplace all dependencies on java.awt into its own package (ui)

## Version 3.1.6

- Fix Serialiser issue with custom objects and String in a custom object
- Fix Progress/ProgressBar synchronisation issues
- Fix Bridge default maxPrintSize parameter

## Version 3.1.5

- Fix Cache with no-parent file
- Fix Progress (Error <> RuntimeException)

## Version 3.1.4

- Fix error handling for tracers in Server

## Version 3.1.3

- Fix ImageUtils.fromStream with non-resetable streams

## Version 3.1.2

- Fix Server regarding the client version passed to the handler
- Improve ServerBridge options

## Version 3.1.1

- Some fixes and trace handling changes in ServerBridge
- Some fixes in Import/Export (objects serialisation)

## Version 3.1.0

- New ServerBridge (including tests)

## Version 3.0.0

- jDoc
- Fix bugs in Server (it was not possible to send objects back to client)
- Improve code in Server (including tests), breaks API
- Remove some deprecated things

## Version 2.2.3

- Fix in readSmallStream
- Change traces handling

## Version 2.2.2

- New method in Cache: manually delete items

## Version 2.2.1

- Small fixes, especially in Progress

## Version 2.2.0

- New classes:
  - Downloader: download URL from recalcitrant websites
  - Cache: manage a local cache

## Version 2.1.0

- Better IOUtils

## Version 2.0.0

- API change
  - IOUtils is now split between itself and ImageUtils -- some changes required in dependant projects
  - Some slight renaming in StringUtils/IOUtils/ImageUtils

- New class ImageText
  - To create ASCII art

## Version 1.6.3

- Version.java
  - Fix toString issues + test + update scripts

## Version 1.6.2

- Version.java
  - Now supports "tag" on the versions (i.e., 0.0.4-niki1 -> tag is "niki", tagVersion is 1)

## Version 1.6.1

- Serialisation utilities
  - Now supports enums and BufferedImages

## Version 1.6.0

- Serialisation utilities
  - Server class to send/receive objects via network easily
  - Serialiser now supports Arrays + fixes

## Version 1.5.1

- Serialisation utilities
  - SerialUtils is now public and can be used to dynamically create an Object
  - The Importer is now easier to use

## Version 1.5.0

- Bundles: change in Bundles and meta data
  - The meta data is more complete now, but it breaks compatibility with both Bundles and @Meta
  - A description can now be added to a bundle item in the graphical editor as a tooltip

- Serialisation utilities
  - A new set of utilities to quickly serialise objects

## Version 1.4.3

- Bugfix: unhtml
  - Also replace non-breakable spaces by normal spaces

## Version 1.4.2

- Bugfix: Deltree
  - Deltree was not OK for files...

## Version 1.4.1

- Progress
  - Better handling of min==max case
  - New methods .done() and .add(int step)

## Version 1.4.0

- R/W Bundles
  - Bundle is now Read/Write

- Bundle Configuration
  - New UI controls to configure the Bundles graphically

## Version 1.3.6

- Fix for Java 1.6 compat
  - Java 1.6 cannot compile it due to variables with ambigous names (which
  - Java 1.8 can identify)

## Version 1.3.5

- Improve ProgressBar UI
  - It now shows all the progression bars of the different steps of progression at the same time

## Version 1.3.4

- Improve TestCase error reporting
  - We know display the full stack trace even for AssertionErrors

- Extends Version
  - ...with new methods: isOlderThan(Version) and isNewerThan(Version)

## Version 1.3.3

- New Version class
  - Which can parse versions from the running program

## Version 1.2.3

- Add openResource and getVersion in IOUtils
  - The file VERSION is supposed to exist

- Give more informartion on AssertErrors
  - The TestCase were not always helpful in case of AssertExceptions; they now print the stacktrace (they only used to do it for non-assert exceptions)

- Fix configure.sh
  - The VERSION file was not added, the Main method was not the correct one (so it was not producing working runnable JAR, yet it stated so)

## Version 1.2.2

- Fix bug in Bundle regarding \t handling
  - ...tests should be written (later)

## Version 1.2.1

- New drawEllipse3D method
  - ...in UIUtils

## Version 1.1.1

- Add UI component for Progress
  - Still a WIP, it only show the current progress bar, still not the children bars (it's planned)

## Version 1.1.0

- Add progress reporting, move to ui package
  - A new progress reporting system (and tests) in the new ui package (some other classes have been moved into ui, too: WrapLayout and UIUtils)

## Version 1.0.0

- Add WrapLayout and UIUtils
  - A FlowLayout that automatically wrap to the next line (from existing code found on internet) and a method to set a fake-native look & feel

## Version 0.9.7

- Improve toImage and allow non-resetable InputStreams
  - ...though they are then automatically saved onto disk then re-opened, then the file is deleted at the end of the process -- bad perfs
  - Worse, it does it even if no EXIF metadata are present (because it cannot know that before reading the Stream, and cannot save a partially, non-resetable Stream to disk)

- Reoarganize some methods from String to IO

## Version 0.9.6

- New test system
  - Now some unit tests have been added, as well as the support classes

## Version 0.9.5

- Resource bundle bug
  - UTF-8 strings were sometimes wrangled
  - It is fixed by using a Bundle#Control, whih sadly is only available in Java 1.6+

## Version 0.9.4

- Compatibility bug
  - Again... because of some useless imports made there for a wrong jDoc comment

## Version 0.9.3

- Compatibility bug
  - The library did not work with JDK versions prior to 1.8 because of a dependency on Base64
  - A new (public domain) class was used instead, which is compatible with Java 1.5 this time

## Version 0.9.2

- Initial version
  - ...on GIT

