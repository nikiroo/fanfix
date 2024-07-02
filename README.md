# Program

Small description.

## Synopsis

- `program --help`

## Description

Long description multiple paragraphs are ok.

## Options

- **--help** (or **-h**): information about the syntax

## Compilation

Just run `make`.  

You can also use those make targets:

- `make doc`: build the Doxygen documentation (`doxygen` required)
- `make man`: build the man page (`pandoc` required)
- `make install PREFIX=/usr/local`: install the program into PREFIX (default is `/usr/local`) and the manual if built
- `make uninstall PREFIX=/usr/local`: uninstall the program from the given PREFIX
- `make clear`: clear the temporary files
- `make mrpropre`: clear everything, including the main executable and the documentation
- `make test`: build the unit tests (`check` required)
- `make run-test`: start the unit tests
- `make run-test-more`: start the extra unit tests (can be long)

## Author

Program was written by Niki Roo <niki@nikiroo.be>

