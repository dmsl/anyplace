#!/bin/bash


# docker network create ros

docker run --rm -it \
    --net host \
    --cap-add=NET_ADMIN \
    --volume="$(dirname `pwd`)":/root/rosjava/src/anyplace-ros \
    ros-kinetic-rosjava \
    bash

# docker network rm ros
# --net ros \
# --name tester_1 \
# --env ROS_HOSTNAME=tester_1 \
# --env ROS_MASTER_URI=http://master:11311 \