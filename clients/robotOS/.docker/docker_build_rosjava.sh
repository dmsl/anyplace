#!/bin/bash

docker build --force-rm -f Dockerfile_rosjava -t ros-kinetic-rosjava --network=host .