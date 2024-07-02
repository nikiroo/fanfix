# Program

Small description.

## Synopsis

- `program --help`

## Description

Long description multiple paragraphs are ok.

## Options

- **--help** (or **-h**): information about the syntax

### Supported platforms

Any platform with at lest Java 1.6 on it should be ok.

It has been tested on Linux (xxx), and YYY (yyy), but feel free to inform us if you try it on another system.

If you have any problems to compile it with a supported Java version (1.6+), please contact us.

## Compilation

Just run `make`.  

You can also use those make targets:

- `make jar`: build the jar file
- `make sjar`: build the source jar file
- `make doc`: build the Doxygen documentation (`doxygen` required)
- `make man`: build the man page (`pandoc` required)
- `make install PREFIX=/usr/local`: install the program into PREFIX (default is `/usr/local`) and the manual if built
- `make uninstall PREFIX=/usr/local`: uninstall the program from the given PREFIX
- `make clear`: clear the temporary files
- `make mrpropre`: clear everything, including the main executable and the documentation
- `make test`: build the unit tests (`check` required)
- `make run-test`: start the unit tests

## Author

Program was written by Niki Roo <niki@nikiroo.be>

