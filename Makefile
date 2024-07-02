#
# Simple makefile that will forward all commands to src/xxx
# > NAME: main program (for 'man' and 'run')
# > NAMES: list of all the programs to compile
# > TESTS: list of all test programs to compile and run
#
NAME   = program
NAMES  = $(NAME) program2
TESTS  = tests-program

################################################################################

# You may override these when calling make
PREFIX = /usr/local
dstdir = bin

.PHONY: all build rebuild run clean mrpropre mrpropre love debug doc man \
	test run-test run-test-more \
	mess-build mess-run mess-clean mess-propre mess-doc mess-man \
	mess-test mess-run-test mess-run-test-more \
	$(NAMES) $(TESTS)

all: build

build: mess-build $(NAMES)

rebuild: mess-rebuild $(NAMES)

test: mess-test $(TESTS)

# Main buildables
$(NAMES) $(TESTS):
	$(MAKE) -C src/$@ $(MAKECMDGOALS) --no-print-directory \
		PREFIX=$(PREFIX) DEBUG=$(DEBUG) dstdir=$(abspath $(dstdir))

# Manual
man: mess-man
	@$(MAKE) -f man.d $(MAKECMDGOALS) NAME=$(NAME)

# Doc
doc: mess-doc
	@$(MAKE) -f doc.d $(MAKECMDGOALS) NAME=$(NAME)

# Run
run: mess-run $(NAME)

# Run main test
run-test: mess-run-test $(TESTS)
run-test-more: mess-run-test-more $(TESTS)

# Misc
love:
	@echo " ...not war."
debug:
	$(MAKE) $(MAKECMDGOALS) PREFIX=$(PREFIX) NAME=$(NAME) DEBUG=1

# Clean
clean: mess-clean doc man $(TESTS) $(NAMES)
mrproper: mrpropre
mrpropre: mess-propre $(TESTS) $(NAMES) doc man

# Install/uninstall
install: mess-install $(NAMES) man
uninstall: mess-uninstall $(NAMES) man

# Messages
mess-build:
	@echo
	@echo ">>>>>>>>>> Building $(NAMES) in $(dstdir)..."
mess-rebuild:
	@echo
	@echo ">>>>>>>>>> Reilding $(NAMES) in $(dstdir)..."
mess-run:
	@echo
	@echo ">>>>>>>>>> Running $(NAME)..."
mess-clean:
	@echo
	@echo ">>>>>>>>>> Cleaning $(NAMES) $(TESTS)..."
mess-propre:
	@echo
	@echo ">>>>>>>>>> Calling Mr Propre..."
mess-doc:
	@echo
	@echo ">>>>>>>>>> Documentation of $(NAME): $(MAKECMDGOALS)..."
mess-man:
	@echo
	@echo ">>>>>>>>>> Manual of $(NAME): $(MAKECMDGOALS)..."
mess-test:
	@echo
	@echo ">>>>>>>>>> Building all tests in $(dstdir): $(TESTS)..."
mess-run-test:
	@echo
	@echo ">>>>>>>>>> Running tests: $(TESTS)..."
mess-run-test-more:
	@echo
	@echo ">>>>>>>>>> Running more tests: $(TESTS)..."
mess-install:
	@echo
	@echo ">>>>>>>>>> Installing $(NAME) into $(PREFIX)..."
mess-uninstall:
	@echo
	@echo ">>>>>>>>>> Uninstalling $(NAME) from $(PREFIX)..."

