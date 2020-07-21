#!/bin/bash
##################################################
# Downloads compiled anyplace from github releases
##################################################
VERSION=$1
GITHUB_REPO=https://github.com/dmsl/anyplace
GITHUB_RELEASES=$GITHUB_REPO/releases/download
URL=$GITHUB_RELEASES/$VERSION/anyplace-server-$VERSION.zip

loc=/opt/app
cache=/opt/cache/anyplace-server-$VERSION.zip

echo "Anyplace compiled sources installation: $VERSION"
if [ -f $cache ]; then
    echo "Anyplace compiled sources: will use cache for $VERSION!"
    cp $cache $loc.zip
else
    echo "Downloading anyplace $VERSION."
    curl -L $URL -o $loc.zip
fi

echo "Unzipping anyplace"
tmp=/opt/_tmp
unzip -q $loc.zip -d $tmp
srcver=$(ls $tmp)

echo mv /opt/_tmp/$srcver $loc
mv /opt/_tmp/$srcver $loc

rmdir /opt/_tmp
