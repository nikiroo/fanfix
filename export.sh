#!/bin/sh

# Export script
# 
# Version:
# - 1.1.1: use the new sjar command to make sources
# - 1.1.0: allow multiple targets
# - 1.0.0: add a version comment

cd "`dirname "$0"`"

if [ "$1" = "" ]; then
	echo "You need to specify where to export it" >&2
	exit 1
fi

LIBNAME="`cat configure.sh | grep '^echo "NAME = ' | cut -d'"' -f2 | cut -d= -f2`"
LIBNAME="`echo $LIBNAME`"

make mrpropre
./configure.sh && make && make sjar
if [ $? = 0 ]; then
	while [ "$1" != "" ]; do
		mkdir -p "$1"/libs/
		cp "$LIBNAME"-`cat VERSION`-sources.jar "$1"/libs/
		cp "$LIBNAME".jar "$1"/libs/
		shift
	done
fi

