#!/bin/sh

# default:
PREFIX=/usr/local
PROGS="java javac jar"

valid=true
while [ "$*" != "" ]; do
	key=`echo "$1" | cut -c1-9`
	val=`echo "$1" | cut -c10-`
	case "$key" in
	--prefix=)
		PREFIX="$val"
	;;
	*)
		echo "Unsupported parameter: '$1'" >&2
		valid=false
	;;
	esac
	shift
done

[ $valid = false ] && exit 1

MESS="A required program cannot be found:"
for prog in $PROGS; do
	out="`whereis -b "$prog" 2>/dev/null`"
	if [ "$out" = "$prog:" ]; then
		echo "$MESS $prog" >&2
		valid=false
	fi
done

[ $valid = false ] && exit 2

if [ "`whereis tput`" = "tput:" ]; then
	ok='"[ ok ]"';
	ko='"[ !! ]"';
	cols=80;
else
	ok='"`tput bold`[`tput setf 2` OK `tput init``tput bold`]`tput init`"';
	ko='"`tput bold`[`tput setf 4` !! `tput init``tput bold`]`tput init`"';
	cols='"`tput cols`"';
fi;


echo "MAIN = be/nikiroo/utils/resources/TransBundle" > Makefile
echo "MORE = be/nikiroo/utils/StringUtils be/nikiroo/utils/IOUtils be/nikiroo/utils/MarkableFileInputStream be/nikiroo/utils/ui/UIUtils be/nikiroo/utils/ui/WrapLayout be/nikiroo/utils/ui/ProgressBar be/nikiroo/utils/test/TestLauncher" >> Makefile
echo "TEST = be/nikiroo/utils/test/Test" >> Makefile
echo "TEST_PARAMS = $cols $ok $ko" >> Makefile
echo "NAME = nikiroo-utils" >> Makefile
echo "PREFIX = $PREFIX" >> Makefile
echo "JAR_FLAGS += -C bin/ be" >> Makefile
echo "SJAR_FLAGS += -C src/ be" >> Makefile

cat Makefile.base >> Makefile

