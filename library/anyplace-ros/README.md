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

## 1. Run the anyplace_ros_client in a docker container 

- Open a new terminal, go to the *.docker* directory and run:

```
./docker_run_publisher.sh
```

This command will run a bash terminal inside a docker container based on the image you created on step 0. The ROS nodes running in this container know the presence of the ROS master and can communicate with it. From this container you should go to the *~/rosjava* directory and run:
```
catkin_make
```
This will build the package once again (hopefully with no errors). After you have built successfully the code, you can deploy the anyplace_ros package by running in the terminal:

```
roslaunch anyplace_ros_pkg anyplace_ros_client.launch
```

This command will initiate a ROS master and then start the anyplace-ros client. This client provides the Anyplace services to the ROS network.
## 2. Test the anyplace_ros package from ROS

- Open a new terminal, go to the *.docker* directory and run:

```bash
./docker_run_tester_1.sh
```

This command will run a bash terminal inside a docker container based on the image you created on step 0. The ROS nodes running in this container know the presence of the ROS master and can communicate with it. In this step we try to test the anyplace_ros with a package created in ROS. From this container you should go to the *~/rosjava* directory and run *catkin_make*. This will build the package once again (hopefully with no errors). After you have built successfully the code, you can deploy the anyplace_ros_request_tester package by running in the terminal:

```bash
roslaunch anyplace_ros_request_tester anyplace_ros_request_tester.launch
```

This will call the /anyplace_ros/estimate_position service of the anyplace_ros_client (deployed in step 1), with testing data. If the call is successful we should monitor an estimated position of our computer.
