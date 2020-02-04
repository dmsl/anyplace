# Anyplace-ROS client
---
Welcome to the tutorial for running the anyplace-ros client which resides in ROS and provides the common API of the Anyplace endpoints to the ROS network.

In order to test the package, a docker container is provided. Thus, for testing the package, the docker software is necessary. Steps to test the anyplace-ros client:

## 0. Create the docker image

- If you haven't done already, go to the *.docker* directory and run:

```
./docker_build_rosjava.sh
```
This command will create a docker image which includes the rosjava packages which are necessary for the anyplace-ros package.

## 1. Run the ROS master in a docker container 

- Go to the *.docker* directory and run:

```
./docker_run_roscore.sh
```
This command will run roscore (the ROS master) inside a docker container based on the image you created on step 0.

## 2. Run a terminal in a docker container 

- Open a new terminal, go to the *.docker* directory and run:

```
./docker_run_publisher.sh
```

This command will run a bash terminal inside a docker container based on the image you created on step 0. The ROS nodes running in this container know the presence of the ROS master and can communicate with it. From this container you can go to the *~/rosjava* directory and run *catkin_make*. This will build the package once again (hopefully with no errors). After you have built successfully the code, you can deploy the anyplace_ros package by running in the terminal:

```
roslaunch anyplace_ros_pkg anyplace_ros.launch
```

## 3. Test the anyplace_ros package from ROS

- Open a new terminal, go to the *.docker* directory and run:

```
./docker_run_tester_1.sh
```

This command will run a bash terminal inside a docker container based on the image you created on step 0. The ROS nodes running in this container know the presence of the ROS master and can communicate with it. In this step we try to test the anyplace_ros with a package created in ROS. Run in a terminal:

```
roslaunch anyplace_ros_pkg_test no_name_yet.launch
```

This will call the /anyplace_ros/estimate_position of the anyplace_ros package (deployed in step 2), with the proper data. If the call is successful we should monitor a 1-second update with the estimated position of our computer.