#!/bin/bash


docker network create ros

docker run --rm -it \
    --net ros \
    --name tester_1 \
    --volume=/home/mike/catkin_ws/src/rb_log_ws/anyplace/anyplace-ros:/root/rosjava/src/anyplace-ros \
    --env ROS_HOSTNAME=tester_1 \
    --env ROS_MASTER_URI=http://master:11311 \
    ros-kinetic-rosjava \
    bash

docker network rm ros