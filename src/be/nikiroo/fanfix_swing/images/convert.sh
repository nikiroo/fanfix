#!/bin/bash

if [ "$1" = "" ]; then
	echo Syntax: "$0 file1.png file2.png..." >&2
	exit 1
fi

while [ "$1" != "" ]; do
	name="`basename "$1" .png`"
	for S in 8 16 24 32 64; do
		convert -resize ${S}x${S} "$name".png "$name"-${S}x${S}.png
	done
	shift
done

