# nikiroo-utils

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

