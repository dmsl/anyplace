#!/bin/bash


docker network create ros

docker run --rm -it \
    --volume="$(dirname `pwd`)":/root/rosjava/src/anyplace-ros \
    --net ros \
    --name master \
    ros-kinetic-rosjava \
    roscore

docker network rm ros
