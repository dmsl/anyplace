#!/bin/bash
VERSION=$1
APP_ENV=$2
DOCKER_TILER=$3

dir_fs="/opt/fs"
dir_app="/opt/app"
dir_app_conf=$dir_app"/conf"
dir_app_bin=$dir_app"/bin"
app_env=$APP_ENV
dir_data_ap=$dir_fs

echo "#######################################"
echo "# Configuring anyplace: $app_env:$VERSION"
echo "#######################################"

dir_conf="/opt/conf/"

if [ ! -d $dir_app ]; then
    echo "ERROR: '$dir_app': app directory is missing!"
    exit 1
fi
if [ ! -d $dir_conf ]; then
    echo "ERROR: '$dir_conf': conf directory is missing!"
    exit 1
fi

if [ ! -f $dir_app_bin/anyplace ]; then
    echo "Anyplace binary not found at: $dir_app_bin"
    ls -l $dir_app_bin
    exit 1
fi

if [ ! -d $DOCKER_TILER ]; then
    echo "Anyplace Tiler not found at: $DOCKER_TILER"
    exit 1
fi


# Copy configuration (couch passwords, etc)
echo "#########################"
echo "➜ Copying configuration.."
echo "#########################"
cp $dir_conf/application.conf $dir_app_conf

# Setup necessary symlinks needed for operation
# NOTE: no need since A4IoT
#echo "#####################"
#echo "➜ Creating symlinks.."
#echo "#####################"

echo "######################"
echo "# Anyplace configured!"
echo "######################"
