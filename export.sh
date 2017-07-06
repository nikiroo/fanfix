#!/bin/sh

# Export script
# 
# Version:
# - 1.0.0: add a version comment

cd "`dirname "$0"`"

if [ "$1" = "" ]; then
	echo "You need to specify where to export it" >&2
	exit 1
elif [ ! -d "$1/libs" ]; then
	echo "The target export directory is not compatible" >&2
	exit 2
fi

LIBNAME="`cat configure.sh | grep '^echo "NAME = ' | cut -d'"' -f2 | cut -d= -f2`"
LIBNAME="`echo $LIBNAME`"

make mrpropre
./configure.sh && make \
	&& cp "$LIBNAME"-`cat VERSION`-sources.jar "$1"/libs/ \
	&& cp "$LIBNAME".jar "$1"/libs/

