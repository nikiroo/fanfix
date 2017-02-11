# Fanfix

A small program to download and convert fanfictions and comics from supported websites into offline files (epu, cbz...).

It can either "convert" a website into a file, or import/export to/from its library.

## Supported platforms

Any platform with at lest Java 1.5 on it should be ok.

It was only tested on Linux up until now, though.

If you have any problems to compile it on another platform or with a supported Java version (1.4 won't work, but you may try to cross-compile; 1.8 had been tested and works), please contact me.

## Usage

- java -jar fanfix.jar --convert http://SOME_SUPPORTE_URL/ epub /home/niki/my-story.epub: will convert the story to EPUB
- java -jar fanfix.jar --import http://SOME_SUPPORTED_URL/ : will import the story into the local library
- java -jar fanfix.jar --export LUID CBZ /tmp/comix.cbz : will export the story from the local library
- java -jar fanfix.jar --list: will list the known stories and their LUIDs from the local library
- ... (calling the program without parameters will display the syntax)

### Environment variables

- LANG=en java -jar fanfix.jar: force the language to English (the only one for now...)
- CONFIG_DIR=$HOME/.fanfix java -jar fanfix.jar: use the given directory as a config directory (and copy the default configuration if needed)
- NOUTF=1 java -jar fanfix.jar: try to fallback to non-unicode values when possible (can have an impact on the resulting files, not only on user messages)

## Compilation

./configure.sh && make

You can also import the java sources into, say, Eclipse, and create a runnable JAR file from there.
Just remember to unpak the 2 dependant libraries before (or "make libs" can do it).

### Dependant libraries (included)

- libs/nikiroo-utils-sources-0.9.1.jar: some shared utility functions I also use elsewhere
- libs/unbescape-1.1.4-sources.jar: a nice library to escape/unescape a lot of text formats; I only use it for HTML

Nothing else but Java 1.5+.

Note that calling "make libs" will export the libraries into the src/ directory.

## Supported websites

Currently, the following websites are supported:
- http://FimFiction.net/: Fanfictions devoted to the My Little Pony show
- http://Fanfiction.net/: Fan fictions of many, many different universes, from TV shows to novels to games.
- http://mangafox.me/: A well filled repository of mangas, or, as their website states: Most popular manga scanlations read online for free at mangafox, as well as a close-knit community to chat and make friends.
- https://e621.net/: Furry website supporting comics, including MLP

We also support some other (file) types:
- epub: EPUB files created by this program (we do not support "all" EPUB files)
- text: Support class for local stories encoded in textual format, with a few rules :
  - the title must be on the first line, 
  - the author (preceded by nothing, "by " or "©") must be on the second line, possibly with the publication date in parenthesis (i.e., "By Unknown (3rd October 1998)"), 
  - chapters must be declared with "Chapter x" or "Chapter x: NAME OF THE CHAPTER", where "x" is the chapter number,
  - a description of the story must be given as chapter number 0,
  - a cover image may be present with the same filename but a PNG, JPEG or JPG extension.
- info_text: Contains the same information as the TEXT format, but with a companion ".info" file to store some metadata
- cbz: CBZ (collection of images) files created with this program

## TODO

- A nice README file
- A binary JAR release (and thus, versions)
- Improve the CLI reader
- Offer some other readers (TUI, GUI)
- Check if it can work on Android

