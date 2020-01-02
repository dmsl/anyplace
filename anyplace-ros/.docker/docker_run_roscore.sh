#!/bin/bash


docker network create ros

docker run --rm -it \
    --volume=/home/mikekaram/catkin_ws/src/rb_log_ws/anyplace/anyplace-ros:/root/rosjava/src/anyplace-ros \
    --net ros \
    --name master \
    ros-kinetic-rosjava \
    roscore

docker network rm ros
