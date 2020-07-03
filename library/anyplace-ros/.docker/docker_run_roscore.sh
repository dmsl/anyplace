#!/bin/bash


docker network create ros

docker run --rm -it \
    --volume="$(dirname `pwd`)":/root/rosjava/src/anyplace-ros \
    --net host \
    ros-kinetic-rosjava \
    roscore

docker network rm ros
# --cap-add=NET_ADMIN \
#     --name master \