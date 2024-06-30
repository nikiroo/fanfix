#
# Java Makefile:
# > NAME: name of project (used for jar output file)
# > MAIN: path to main java source to compile
# > MORE: path to supplementary needed resources not linked from MAIN
# > TEST: path to main test source to compile
# > JAR_FLAGS: a list of things to pack, each usually prefixed with "-C bin/"
# > SJAR_FLAGS: like JAR_FLAGS, but for *-sources.jar
# > PREFIX: usually /usr/local (where to install the program)
#
NAME  = fanfix
MAIN  = be/nikiroo/fanfix/Main
MORE += be/nikiroo/utils/ui/ImageUtilsAwt
TEST  = be/nikiroo/fanfix/test/Test

JAR_MISC = -C ./ LICENSE -C ./ VERSION -C libs/ licenses
JAR_FLAGS  += -C bin/ be -C bin/ org $(JAR_MISC)
SJAR_FLAGS += -C src/ org -C src/ be $(JAR_MISC)

PREFIX = /usr/local

# Makefile base template
# 
# Version:
# - 1.0.0: add a version comment
# - 1.1.0: add 'help', 'sjar'
# - 1.2.0: add 'apk'
# - 1.2.1: improve 'apk' and add 'android'
# - 1.3.0: add 'man' for man(ual) pages
# - 1.4.0: remove android stuff (not working anyway)
# - 1.5.0: include sources and readme/changelog in jar
# - 1.5.1: include binaries from libs/bin/ into the jar
# - 1.6.0: rework the system without need of a ./configure.sh, add uninstall


JAVAC = javac
JAVAC_FLAGS += -encoding UTF-8 -d ./bin/ -cp ./src/
JAVA = java
JAVA_FLAGS += -cp ./bin/
JAR = jar
RJAR = java
RJAR_FLAGS += -jar

all: build jar man

help:
	@echo "Usual options:"
	@echo "=============="
	@echo "	make		: to build the jar file and man pages IF possible"
	@echo "	make help	: to get this help screen"
	@echo "	make libs	: to update the libraries into src/"
	@echo "	make build	: to update the binaries (not the jar)"
	@echo "	make test	: to update the test binaries"
	@echo "	make build jar	: to update the binaries and jar file"
	@echo "	make sjar	: to create the sources jar file"
	@echo "	make clean	: to clean the directory of intermediate files"
	@echo "	make mrpropre	: to clean the directory of all outputs"
	@echo "	make run	: to run the program from the binaries"
	@echo "	make run-test	: to run the test program from the binaries"
	@echo "	make jrun	: to run the program from the jar file"
	@echo "	make install	: to install the application into $$PREFIX"
	@echo "	make uninstall	: to uninstall the application from $$PREFIX"
	@echo " make man	: to make the manual pages (requires pandoc)"

.PHONY: all clean mrproper mrpropre build run jrun jar sjar resources test-resources install libs man love

bin:
	@mkdir -p bin

jar: $(NAME).jar

sjar: $(NAME)-sources.jar

build: resources
	@echo Compiling program...
	@echo "	src/$(MAIN)"
	@$(JAVAC) $(JAVAC_FLAGS) "src/$(MAIN).java"
	@[ "$(MORE)" = "" ] || for sup in $(MORE); do \
		echo "	src/$$sup" ;\
		$(JAVAC) $(JAVAC_FLAGS) "src/$$sup.java" ; \
	done

test: test-resources
	@[ -e bin/$(MAIN).class ] || echo You need to build the sources
	@[ -e bin/$(MAIN).class ]
	@echo Compiling test program...
	@[ "$(TEST)" != "" ] || echo No test sources defined.
	@[ "$(TEST)"  = "" ] || for sup in $(TEST); do \
		echo "	src/$$sup" ;\
		$(JAVAC) $(JAVAC_FLAGS) "src/$$sup.java" ; \
	done

clean:
	rm -rf bin/
	@echo Removing sources taken from libs...
	@for lib in libs/*-sources.jar libs/*-sources.patch.jar; do \
		if [ "$$lib" != 'libs/*-sources.jar' \
				-a "$$lib" != 'libs/*-sources.patch.jar' ]; \
		then \
			basename "$$lib"; \
			jar tf "$$lib" | while read -r ln; do \
				[ -f "src/$$ln" ] && rm "src/$$ln"; \
			done; \
			jar tf "$$lib" | tac | while read -r ln; do \
				if [ -d "src/$$ln" ]; then \
					rmdir "src/$$ln" 2>/dev/null || true; \
				fi; \
			done; \
		fi \
	done

mrproper: mrpropre
mrpropre: clean man
	rm -f $(NAME).jar
	rm -f $(NAME)-sources.jar
	[ ! -e VERSION ] || rm -f "$(NAME)-`cat VERSION`.jar"
	[ ! -e VERSION ] || rm -f "$(NAME)-`cat VERSION`-sources.jar"

love:
	@echo "	...not war."

resources: libs
	@echo Copying resources and documentation into bin/...
	@if ! cp *.md bin/ 2>/dev/null; then \
		if [ -e VERSION ]; then \
			cp VERSION bin/no-documentation.md; \
		else \
			echo > bin/no-documentation.md; \
		fi; \
	fi
	@cd src && find . | grep -v '\.java$$' \
		| grep -v '/test/' | while read -r ln; do \
		if [ -f "$$ln" ]; then \
			dir="`dirname "$$ln"`"; \
			mkdir -p "../bin/$$dir" ; \
			cp "$$ln" "../bin/$$ln" ; \
		fi ; \
	done
	@[ ! -e VERSION ] || cp VERSION bin/

test-resources: resources
	@echo Copying test resources into bin/...
	@cd src && find . | grep -v '\.java$$' \
		| grep '/test/' | while read -r ln; do \
		if [ -f "$$ln" ]; then \
			dir="`dirname "$$ln"`"; \
			mkdir -p "../bin/$$dir" ; \
			cp "$$ln" "../bin/$$ln" ; \
		fi ; \
	done

libs: bin
	@if [ ! -e bin/libs -a -d libs ]; then \
		echo Extracting sources from libs...; \
		cd src; \
		for lib in ../libs/*-sources.jar \
				../libs/*-sources.patch.jar; do \
			if [ "$$lib" != '../libs/*-sources.jar' \
				-a "$$lib" != '../libs/*-sources.patch.jar' ]; \
			then \
				basename "$$lib"; \
				jar xf "$$lib"; \
			fi; \
		done; \
	fi;
	@[ ! -d libs ] || touch bin/libs

$(NAME)-sources.jar: libs
	@echo Making sources JAR file...
	@echo > bin/manifest
	@if [ "$(SJAR_FLAGS)" = "" ]; then \
		echo No sources JAR file defined, skipping; \
	else \
		echo Creating $(NAME)-sources.jar...; \
		$(JAR) cfm $(NAME)-sources.jar \
			bin/manifest -C ./ *.md $(SJAR_FLAGS); \
		if [ -e VERSION ]; then \
			echo Copying to "$(NAME)-`cat VERSION`-sources.jar"...;\
			cp $(NAME)-sources.jar \
				"$(NAME)-`cat VERSION`-sources.jar"; \
		fi; \
	fi;

$(NAME).jar: build resources
	@if [ -d libs/bin/ ]; then \
		echo "Copying additional binaries from libs/bin/ into bin/...";\
		cp -r libs/bin/* bin/; \
	fi;
	@echo "Copying sources into bin/..."
	@cp -r src/* bin/
	@echo "Making jar..."
	@echo "Main-Class: `echo "$(MAIN)" | sed 's:/:.:g'`" > bin/manifest
	@echo >> bin/manifest
	$(JAR) cfm $(NAME).jar bin/manifest -C ./ *.md $(JAR_FLAGS)
	@[ ! -e VERSION ] || echo Copying to "$(NAME)-`cat VERSION`.jar"...
	@[ ! -e VERSION ] || cp $(NAME).jar "$(NAME)-`cat VERSION`.jar"

run: build
	@echo Running "$(NAME)"...
	$(JAVA) $(JAVA_FLAGS) $(MAIN)

jrun: build
	@echo Running "$(NAME).jar"...
	$(RJAR) $(RJAR_FLAGS) $(NAME).jar

run-test: test
	@echo Running tests for "$(NAME)"...
	@[ "$(TEST)" != "" ] || echo No test sources defined.
	@if [ "`whereis tput`" = "tput:" ]; then \
	ok='"[ ok ]"'; \
	ko='"[ !! ]"'; \
	cols=80; \
	else \
	ok="`tput bold`[`tput setf 2` OK `tput init``tput bold`]`tput init`"; \
	ko="`tput bold`[`tput setf 4` !! `tput init``tput bold`]`tput init`"; \
	cols='"`tput cols`"'; \
	fi; \
	[ "$(TEST)"  = "" ] || \
		( clear ; $(JAVA) $(JAVA_FLAGS) $(TEST) "$$cols" "$$ok" "$$ko" )

install: man
	@[ -e $(NAME).jar ] || echo You need to build the jar
	@[ -e $(NAME).jar ]
	mkdir -p "$(PREFIX)/lib" "$(PREFIX)/bin"
	cp $(NAME).jar "$(PREFIX)/lib/"
	( \
	echo "#!/bin/sh"; \
	echo "$(RJAR) $(RJAR_FLAGS) \"$(PREFIX)/lib/$(NAME).jar\" \"\$$@\"" \
	) > "$(PREFIX)/bin/$(NAME)"
	chmod a+rx "$(PREFIX)/bin/$(NAME)"

uninstall: man
	rm "$(PREFIX)/bin/$(NAME)"
	rm "$(PREFIX)/lib/$(NAME).jar"
	rmdir "$(PREFIX)/bin" 2>/dev/null

man: 
	@$(MAKE) -f man.d $(MAKECMDGOALS) NAME=$(NAME)

