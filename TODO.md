My current planning for Fanfix (but not everything appears on this list):
- [ ] Support new websites
  - [x] YiffStar
  - [ ] [Two Kinds](http://twokinds.keenspot.com/)
  - [ ] [Slightly damned](http://www.sdamned.com/)
  - [x] New API on FimFiction.net (faster)
  - [ ] Others? Any ideas? I'm open for requests
    - [x] [e-Hentai](https://e-hentai.org/) requested
    - [ ] Find some FR comics/manga websites
- [x] A GUI library
  - [x] Make one
  - [x] Make it run when no args passed
  - [x] Fix the UI, it is ugly
  - [x] Work on the UI thread is BAD
  - [x] Allow export
  - [x] Allow delete/refresh
  - [x] Show a list of types
    - [x] ..in the menu
    - [x] ..as a screen view
  - [x] options screen
  - [x] support progress events
- [ ] A TUI library
  - [x] Choose an output (Jexer)
  - [x] Implement it from --set-reader to the actual window
  - [x] List the stories
  - [ ] Fix the UI layout
  - [x] Status bar
  - [ ] Real menus
  - [x] Open a story in the reader and/or natively
  - [ ] Update the screenshots
  - [ ] Remember the current chapter and current read status of stories
  - [ ] support progress events
- [x] Network support
  - [x] A server that can send the stories
  - [x] A network implementation of the Library
  - [x] Write access to the library
  - [x] Access rights (a simple "key")
  - [x] More tests, especially with the GUI
    - [x] ..even more
  - [x] support progress events
- [x] Check if it can work on Android
  - [x] First checks: it should work, but with changes
  - [x] Adapt work on images :(
  - [x] Partial/Conditional compilation
  - [x] APK export
- [ ] Android
  - [x] Android support
  - [x] Show current stories
  - [x] Download new stories
  - [ ] Sort stories by Source/Author
  - [ ] Fix UI
  - [ ] support progress events
- [ ] Translations
  - [x] i18n system in place
  - [x] Make use of it
  - [ ] Use it for all user output
  - [ ] French translation
  - [x] French manual/readme
- [x] Install a mechanism to handle stories import/export progress update
  - [x] Progress system
  - [x] in support classes (import)
  - [x] in output classes (export)
- [x] Version
  - [x] Use a version number
  - [x] Show it in UI
  - [x] A check-update feature
    - [x] ..translated
- [ ] Improve GUI library
    - [x] Allow lauching a custom application instead of Desktop.start
    - [ ] Add the resume next to the cover icon if available (as an option)
- [ ] Bugs
    - [x] Fix "Redownload also reset the source"
    - [ ] Fix eHentai "content warning" access
    - [ ] Fix the configuration system (for new or changed options, new or changed languages)
    - [ ] remote import also download the file in cache, why?
    - [ ] import file in remote mode tries to import remote file!!
    - [ ] import file does not find author in cbz with SUMMARY file
    - [ ] import file:// creates a tmp without auto-deletion in /tmp/fanfic-...
