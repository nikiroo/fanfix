# Fanfix

# Version 1.6.4

- update nikiroo-utils
- android: Android compatibility
- MangaFox: fix after website update
- new: use jsoup (only for MangaFox for now)

# Version 1.6.3

- fix: bug fixes
- remote: progress report
- remote: can send large files
- remote: detect server state
- remote: import and change source on server
- CBZ: better support for some CBZ files (if SUMMARY or URL files are present in it)
- Library: fix cover images not deleted on story delete
- fix: some images not supported because not jpeg-able (now try again in png)
- remote: fix some covers not found over the wire (nikiroo-utils)
- remote: fix cover image files not sent over the wire

## Version 1.6.2

- GUI: better progress bars
- GUI: can now open files much quicker if they are stored in both library and cache with the same output type 

## Version 1.6.1

- GUI: new option (disabled by default) to show one item per source type instead of one item per story when showing ALL sources (which is also the start page)
- fix: source/type reset when redownloading
- GUI: show the number of images instead of the number of words for images documents 

## Version 1.6.0

- TUI: new TUI (text with windows and menus) -- not compiled by default (configure.sh)
- remote: a server option to offer stories on the network
- remote: a remote library to get said stories from the network
- update to latest version of nikiroo-utils
- FimFiction: support for the new API
- new: cache update (you may want to clear your current cache)
- GUI: bug fixed (moving an unopened book does not fail any more)

## Version 1.5.3

- FimFiction: Fix tags and chapter handling for some stories

## Version 1.5.2

- FimFiction: Fix tags metadata on FimFiction 4

## Version 1.5.1

- FimFiction: Update to FimFiction 4
- eHentai: Fix some meta data that were missing

## Version 1.5.0

- eHentai: new website supported on request (do not hesitate!): e-hentai.org
- Library: perf improvement when retrieving the stories (cover not loaded when not needed)
- Library: fix the covers that were not always removed when deleting a story
- GUI: perf improvement when displaying books (cover resized then cached)
- GUI: sources are now editable ("Move to...")

## Version 1.4.2

- GUI: new Options menu to configure the program (minimalist for now)
- new: improve progress reporting (smoother updates, more details)
- fix: better cover support for local files

## Version 1.4.1

- fix: UpdateChecker which showed the changes of ALL versions instead of the newer ones only
- fix: some bad line breaks on HTML support (including FanFiction.net)
- GUI: progress bar now working correctly
- update: nikiroo-utils update to show all steps in the progress bars
- ( --End of changes for version 1.4.1-- )

## Version 1.4.0

- new: remember the word count and the date of creation of Fanfix stories
- GUI: option to show the word count instead of the author below the book title
- CBZ: do not include the first page twice anymore for no-cover websites
- GUI: update version check (we now check for new versions)

## Version 1.3.1

- GUI: can now display books by Author

## Version 1.3.0

- YiffStar: YiffStar (SoFurry.com) is now supported
- new: supports login/password websites
- GUI: copied URLs (ctrl+C) are selected by default when importing a URL
- GUI: version now visible (also with --version)

## Version 1.2.4

- GUI: new option: Re-download
- GUI: books are now sorted (will not jump around after refresh/redownload)
- fix: quote character handling
- fix: chapter detection
- new: more tests included

## Version 1.2.3

- HTML: include the original (info_text) files when saving
- HTML: new input type supported: HTML files made by Fanfix

## Version 1.2.2

- GUI: new "Save as..." option
- GUI: fixes (icon refresh)
- fix: handling of TABs in user messages
- GUI: LocalReader can now be used with --read
- ePub: CSS style fixes

## Version 1.2.1

- GUI: some menu functions added
- GUI: right-click popup menu added
- GUI: fixes, especially for the LocalReader library
- GUI: new green round icon to denote "cached" (into LocalReader library) files

## Version 1.2.0

- GUI: progress reporting system
- ePub: CSS style changes
- new: unit tests added
- GUI: some menu functions added (delete, refresh, a place-holder for export)

## Version 1.1.0

- CLI: new Progress reporting system
- e621: fix on "pending" pools, which were not downloaded before
- new: unit tests system added (but no test yet, as all tests were moved into nikiroo-utils)

## Version 1.0.0

- GUI: it is now good enough to be released (export is still CLI-only though)
- fix: bug fixes
- GUI: improved (a lot)
- new: should be good enough for 1.0.0

## Version 0.9.5

- fix: bug fixes
- new: WIN32 compatibility (tested on Windows 10)

## Version 0.9.4

- fix: bug fixes (lots of)
- new: perf improved
- new: use less cache files
- GUI: improvement (still not really OK, but OK enough I guess)

## Version 0.9.3

- fix: bug fixes (lots of)
- GUI: first implementation (which is ugly and buggy -- the buggly GUI)

## Version 0.9.2

- new: minimum JVM version: Java 1.6 (all binary JAR files will be released in 1.6)
- fix: bug fixes

## Version 0.9.1

- initial version

