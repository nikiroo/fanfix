# Fanfix

# Version WIP

- Bug fixes
- Remote server/client improvements (progress report, can send large files)
- Better support for some CBZ files (if SUMMARY or URL files are present in it)
- Fix cover images not deleted on story delete
- Fix some images not supported because not jpeg-able (now try again in png)
- Fix some covers not found over the wire (nikiroo-utils)

## Version 1.6.2

- Better progress bars
- GUI can now open files much quicker if they are stored in both library and cache with the same output type 

## Version 1.6.1

- New option (disabled by default) to show one item per source type in GUI instead of one item per story when showing ALL sources (which is also the start page)
- Fix source/type reset when redownloading
- Show the number of images instead of the number of words for images documents 

## Version 1.6.0

- TUI (text with windows and menus) -- not compiled by default (configure.sh)
- A server option to offer stories on the network
- A remote library to get said stories from the network
- Update to latest version of nikiroo-utils
- Support for FimFiction.net via the new API
- Cache update (you may want to clear your current cache)
- Bug fixed (moving an unopened book in GUI mode does not fail any more)

## Version 1.5.3

- FimFiction: Fix tags and chapter handling for some stories

## Version 1.5.2

- Fix tags metadata on FimFiction 4

## Version 1.5.1

- Update to FimFiction 4
- Fix some meta data that were missing on e-Hentai

## Version 1.5.0

- New website supported following a request: e-hentai.org
- Library: perf improvement when retrieving the stories (cover not loaded when not needed)
- Library: fix the covers that were not always removed when deleting a story
- UI: perf improvement when displaying books (cover resized then cached)
- UI: Sources are now editable ("Move to...")

## Version 1.4.2

- New Options menu in UI to configure the program (minimalist for now)
- Improve progress reporting (smoother updates, more details)
- Better cover support for local files

## Version 1.4.1

- Fix UpdateChecker which showed the changes of ALL versions instead of the newer ones only
- Fix some bad line breaks on HTML supports (including FanFiction.net)
- UI: progress bar now working correctly
- nikiroo-utils update to show all steps in the progress bars
- ( --End of changes for version 1.4.1-- )

## Version 1.4.0

- Remember the word count and the date of creation of Fanfix stories
- UI: option to show the word count instead of the author below the book title
- CBZ: do not include the first page twice anymore for no-cover websites
- UI: update version check (we now check for new versions)

## Version 1.3.1

- UI: can now display books by Author

## Version 1.3.0

- now supports YiffStar (SoFurry.com)
- supports login/password websites
- UI: copied URLs (ctrl+C) are selected by default when importing an URL
- UI: version now visible in UI (also with --version)

## Version 1.2.4

- new UI option: Re-download
- fix UI: books are now sorted (will not jump around after refresh/redownload)
- fixes on quote character handling
- fixes on Chapter detection
- more tests included

## Version 1.2.3

- Include the original (info_text) files when saving to HTML
- New input type supported: HTML files made by Fanfix

## Version 1.2.2

- New "Save as..." GUI option
- GUI fixes (icon refresh)
- Fix handling of TABs in user messages
- LocalReader can now be used with --read
- Some fixes in CSS

## Version 1.2.1

- Some GUI menu functions added
- Right-click popup menu added
- GUI fixes, especially for the LocalReader library
- New green round icon to denote "cached" (into LocalReader library) files

## Version 1.2.0

- Progress reporting system in GUI, too
- CSS style changes
- unit tests added
- Some GUI menu functions added (delete, refresh, a place-holder for export)

## Version 1.1.0

- new Progress reporting system (currently only in CLI mode)
- fix on e621 for "pending" pools, which were not downloaded before
- unit tests system added (but no test yet, as all tests were moved into nikiroo-utils)

## Version 1.0.0

- the GUI is now good enough to be released (export is still CLI-only though)
- bugs fixed
- GUI improved (a lot)
- should be good enough for 1.0.0

## Version 0.9.5

- bugs fixed
- WIN32 compatibility (tested on Windows 10)

## Version 0.9.4

- bugs fixed (lots of)
- perf improved
- use less cache files
- GUI improvement (still not really OK, but OK enough I guess)

## Version 0.9.3

- bugs fixed (lots of)
- first GUI implementation (which is ugly and buggy -- the buggly GUI)

## Version 0.9.2

- minimum JVM version: Java 1.6 (all binary JAR files will be released in 1.6)
- bugs fixed

## Version 0.9.1

- initial version

