#!/bin/bash

can_use_as_dir() { 
	(cd ${1:?"$1"}) || exit 1
}

######################### MAIN ########################

[[ "$#" != 1 ]] && echo "No cleaning has happened!" && exit 1

# navigate to the correct directory
cd "$(dirname "$1")"

# make sure the TILES directory exists or create it
TILES_DIR="static_tiles"
if [ ! -e $TILES_DIR ]; then
	echo "Creating tiles directory..."
	mkdir $TILES_DIR
fi
# verify the creation of the tile dir
can_use_as_dir $TILES_DIR

# the tile files have names with convention: z[ZOOM]x[TILE_X]y[TILE_Y].png
REGEX="^z[0-9]+x[0-9]+y[0-9]+[.]png$"
for filename in `ls`
do
	if [[ (-f "$filename") && ("$filename" =~ $REGEX) ]]; then
		zoom=$( basename $filename | sed 's/z\([0-9]\+\)x[0-9]\+y[0-9]\+.png/\1/g' )
		echo "$filename - zoom: $zoom"

		# ensure that the directory for this zoom level exists
		zoomDir="$TILES_DIR/$zoom"
		echo $zoomDir
		if [ ! -e $zoomDir ]; then
			echo "Creating directory for zoom: $zoom"
			mkdir $zoomDir
		fi
		# verify the creation of zoom dir
		can_use_as_dir $zoomDir

		# transfer the tile to its directory
		mv $filename "$zoomDir/$filename"
	fi
done


# find the Coordinates bounds of each zoom. 
# It will greatly speed up the android app while checking for available Tile
boundsF="$TILES_DIR/bounds.txt"
rm -f "$boundsF" 2> /dev/null
for zoom in $( ls "$TILES_DIR" )
do
	if [[ "$zoom" =~ [^0-9]+ ]]; then
		continue
	fi

	echo $zoom
	zoomDir="$TILES_DIR/$zoom"
	minX=-1
	maxX=0
	minY=-1
	maxY=0
	for tile in $( ls "$zoomDir" )
	do
		x=$( echo "$tile" | sed 's/z[0-9]\+x\([0-9]\+\)y[0-9]\+.png/\1/' )
		y=$( echo "$tile" | sed 's/z[0-9]\+x[0-9]\+y\([0-9]\+\).png/\1/' )
		echo "$x,$y"
		if [[ "$x" -gt "$maxX" ]]; then
			maxX=$x
		fi
		if [[ "$minX" -eq "-1" || "$x" -lt "$minX" ]]; then
			minX=$x
		fi
		if [[ "$y" -gt "$maxY" ]]; then
			maxY=$y
		fi
		if [[ "$minY" -eq "-1" || "$y" -lt "$minY" ]]; then
			minY=$y
		fi
	done
	echo "$zoom,$minX,$maxX,$minY,$maxY" >> "$boundsF"
done	

# create the archive that contains all the necessary tiles and meta data file
cd "$TILES_DIR"
ls | grep -e "[^(.zip)]$" | xargs zip -r tiles_archive.zip 
cd ..

# delete the temporary images in the dir
rm -f padded-image.png zoom-sized-image-z*.png

echo -e "Everything has been cleaned up!"
