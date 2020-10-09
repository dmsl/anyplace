#!/bin/bash

LOG_ENABLED=0

get_abs_path(){
	[[ -z "$1" ]] && echo "get_abs_path:: 1 argument required" && exit 1 
	local PARENT_DIR=$(dirname "$1")
	# navigate to the direcotry of the argument
	cd "$PARENT_DIR"
	local abs_path="$(pwd)"/"$(basename "$1")"
	# return to the directory we execute
	cd - >/dev/null
	echo "$abs_path"
}

get_abs_path2(){
	[[ -z "$1" ]] && echo "get_abs_path:: 1 argument required" && exit 1 
	local arg_path=$(readlink -f $1)
	#local abs_parent_path=$(dirname "$arg_path")
	#echo "$abs_parent_path"
	echo "$arg_path"
}


get_scripts_dir(){
	local abs_path=$(get_abs_path "$0")
	echo $(dirname "$abs_path")
}

get_scripts_dir2(){
	SOURCE="${BASH_SOURCE[0]}"
	while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
		DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
	    	SOURCE="$(readlink "$SOURCE")"
	      	[[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
      	done
      	DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
      	echo "$DIR"
}

usage(){
	echo " ::: Usage ::: "
	echo -e "./start-anyplace-tiler.sh <ImageFilePath> <ImageLatitude> <ImageLongitude> [-DISLOG/-ENLOG]"
	echo
	exit 1
}

check_for_errors(){
	if [[ $? -ne 0 ]]; then
		echo "There was a problem running the anyplace-tiler.py script!"
		echo "Log: $LOG"
		exit 1
	fi
}

check_requirements(){
	[[ -z "$(which python)" ]] && echo "python ::You do not have Python installed!" && exit 1
	[[ -z "$(which convert)" ]] && echo "convert ::You do not have ImageMagick installed!" && exit 1
	[[ -z "$(which identify)" ]] && echo "identify ::You do not have ImageMagick installed!" && exit 1
	[[ -z "$(which advpng)" ]] && echo "advpng ::You do not have AdvanceCOMP installed!" && exit 1
}

##################### MAIN

[[ "$#" != "5" ]] && usage

[[ "$4" != "-ENLOG" && "$4" != "-DISLOG" ]] && usage

# TODO
# DO MORE ROBUST CHECK FOR THE ARGUMENTS

if [[ "$4" == "-ENLOG" ]]; then
	LOG_ENABLED=1
else
	LOG_ENABLED=0
fi

scriptsDir=$( get_scripts_dir )
imagePath=$( get_abs_path2 $1 )
imageDir=$( dirname "$imagePath" )

LOG="$imageDir/anyplace_tiler_$(date +"%Y_%m_%d_%s").log"
# save Input to 3 and output to 4 and redirect them to the LOG file
if [[ $LOG_ENABLED -eq 1 ]]; then
	exec 3>&1 4>&2 1>$LOG 2>&1
fi

echo
echo ":: INITIATING TILING PROCESS ..."

echo "Checking requirements ..."
check_requirements

echo "Scripts Directory: $scriptsDir"
echo "Image: $imagePath"

ImageLatitude="$2"
ImageLongitude="$3"
ZoomOriginal=22
ZoomDestination=19
ImageFileName="$imagePath"
UploadZoom="$5"


echo
echo ":: Starting anyplace-tiler ..."
anyTiler="$scriptsDir/anyplace-tiler.py" 

python "$anyTiler" "$scriptsDir" "$ImageLatitude" "$ImageLongitude" "$ZoomOriginal" "$ZoomDestination" "$ImageFileName" "$UploadZoom"
check_for_errors

echo
echo ":: Finished Tiling! ::"

# restore the input and output
if [[ $LOG_ENABLED -eq 1 ]]; then
	exec 1>&3 2>&4
fi

echo "Log: $LOG"
