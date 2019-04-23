#!/bin/sh

# default:
PREFIX=/usr/local
PROGS="java javac jar"

UI=be/nikiroo/utils/ui/test/TestUI
JUI="-C bin/ be/nikiroo/utils/ui"
ANDOIRD=
JANDROID=

valid=true
while [ "$*" != "" ]; do
	key=`echo "$1" | cut -f1 -d=`
	val=`echo "$1" | cut -f2 -d=`
	case "$key" in
	--help) #		This help message
		echo The following arguments can be used:
		cat "$0" | grep '^\s*--' | grep '#' | while read ln; do
			cmd=`echo "$ln" | cut -f1 -d')'`
			msg=`echo "$ln" | cut -f2 -d'#'`
			echo "	$cmd$msg"
		done
	;;
	--prefix) #=PATH	Change the prefix to the given path
		PREFIX="$val"
	;;
	--ui) #=no	Disable UI (Swing/AWT) support
		[ "$val" = no -o "$val" = false ] && UI= && JUI=
		if [ "$val" = yes -o "$val" = true ]; then
			UI=be/nikiroo/utils/ui/test/TestUI
			JUI="-C bin/ be/nikiroo/utils/ui"
		fi
	;;
	--android) #=yes	Enable Android UI support
		[ "$val" = no -o "$val" = false ] && ANDROID= && JANDROID=
		if [ "$val" = yes -o "$val" = true ]; then
			ANDROID=be/nikiroo/utils/android/test/TestAndroid
			JANDROID="-C bin/ be/nikiroo/utils/android"
		fi
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


echo "MAIN = be/nikiroo/utils/test_code/Test" > Makefile
echo "MORE = $UI $ANDROID" >> Makefile
echo "TEST = be/nikiroo/utils/test_code/Test" >> Makefile
echo "TEST_PARAMS = $cols $ok $ko" >> Makefile
echo "NAME = nikiroo-utils" >> Makefile
echo "PREFIX = $PREFIX" >> Makefile
echo "JAR_FLAGS += -C bin/ be $JUI $JANDROID -C bin/ VERSION" >> Makefile
echo "SJAR_FLAGS += -C src/ org -C src/ be" >> Makefile

cat Makefile.base >> Makefile

