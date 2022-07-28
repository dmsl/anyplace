/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cy.ac.ucy.cs.anyplace.ros;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.node.service.ServiceServer;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cy.ac.ucy.anyplace.Anyplace;
import anyplace_ros_custom_msgs.*;

/**
 * A simple {@link ServiceServer} {@link NodeMain}. The original code is created
 * by: @author damonkohler@google.com (Damon Kohler) The custom implementation
 * is created by: mickaram@hotmail.com (Mike Karamousadakis)
 */
public class Client extends AbstractNodeMain {

	private String root_namespace = "/anyplace_ros/";

	private String host;

	private String port;

	private String cache;

	private Boolean debug;

	private Anyplace client;

	private String access_token;

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("anyplace_ros_client/server");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {

		ParameterTree parameterTree = connectedNode.getParameterTree();

		host = parameterTree.getString(root_namespace + "host", "ap-dev.cs.ucy.ac.cy");
		port = parameterTree.getString(root_namespace + "port", "443");
		cache = parameterTree.getString(root_namespace + "cache", "/res");
		access_token = parameterTree.getString(root_namespace + "access_token");
		debug = parameterTree.getBoolean(root_namespace + "anyplace_ros_debug", false);

		if (access_token.isEmpty() || access_token == null) {
			connectedNode.getLog().error(
					"Access token is empty. Could not initialize correctly the anyplace-ros client. Check that the file params.yaml can be accessed.\n");
			System.exit(-1);
		}
		client = new Anyplace(host, port, cache);

		if (client != null && debug == true) {
			connectedNode.getLog().info("Client info: \n host: " + host + "\n port: " + port + "\n cache: " + cache
					+ "\n access_token: " + access_token + "\n debug: " + debug.toString() + " \n");
		}

		/***************************************************
		 * PoiDetails Service Define PoiDetails and it's callback
		 ****************************************************/
		ServiceResponseBuilder<PoiDetailsRequest, PoiDetailsResponse> PoiDetailsService = new ServiceResponseBuilder<PoiDetailsRequest, PoiDetailsResponse>() {
			@Override
			public void build(PoiDetailsRequest request, PoiDetailsResponse response) {
				String pois = request.getPois();
				if (pois.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.poiDetails(access_token, pois);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "poi_details", PoiDetails._TYPE, PoiDetailsService);

		/***************************************************
		 * NavigationXY Service Define NavigationXY and it's callback
		 ****************************************************/
		ServiceResponseBuilder<NavigationXYRequest, NavigationXYResponse> NavigationXYService = new ServiceResponseBuilder<NavigationXYRequest, NavigationXYResponse>() {
			@Override
			public void build(NavigationXYRequest request, NavigationXYResponse response) {
				String buid = request.getBuid();
				String floor = request.getFloor();
				String pois_to = request.getPoisTo();
				String coordinates_la = request.getCoordinatesLa();
				String coordinates_lo = request.getCoordinatesLo();
				if (buid.isEmpty() || floor.isEmpty() || pois_to.isEmpty() || coordinates_la.isEmpty()
						|| coordinates_lo.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.navigationXY(access_token, pois_to, buid, floor, coordinates_la,
						coordinates_lo);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "navigation_xy", NavigationXY._TYPE, NavigationXYService);

		/***************************************************
		 * NavPoiToPoi Service Define NavPoiToPoi and it's callback
		 ****************************************************/
		ServiceResponseBuilder<NavPoiToPoiRequest, NavPoiToPoiResponse> NavPoiToPoiService = new ServiceResponseBuilder<NavPoiToPoiRequest, NavPoiToPoiResponse>() {
			@Override
			public void build(NavPoiToPoiRequest request, NavPoiToPoiResponse response) {
				String pois_from = request.getPoisFrom();
				String pois_to = request.getPoisTo();
				if (pois_from.isEmpty() || pois_to.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.navigationPoiToPoi(access_token, pois_to, pois_from);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "nav_poi_to_poi", NavPoiToPoi._TYPE, NavPoiToPoiService);

		/***************************************************
		 * BuildingAll Service Define BuildingAll and it's callback
		 ****************************************************/
		ServiceResponseBuilder<BuildingAllRequest, BuildingAllResponse> BuildingAllService = new ServiceResponseBuilder<BuildingAllRequest, BuildingAllResponse>() {
			@Override
			public void build(BuildingAllRequest request, BuildingAllResponse response) {
				String anyplace_response = client.buildingAll();
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "building_all", BuildingAll._TYPE, BuildingAllService);

		/***************************************************
		 * BuildingsByCampus Service Define BuildingsByCampus and it's callback
		 ****************************************************/
		ServiceResponseBuilder<BuildingsByCampusRequest, BuildingsByCampusResponse> BuildingsByCampusService = new ServiceResponseBuilder<BuildingsByCampusRequest, BuildingsByCampusResponse>() {
			@Override
			public void build(BuildingsByCampusRequest request, BuildingsByCampusResponse response) {
				String cuid = request.getCuid();
				if (cuid.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.buildingsByCampus(cuid);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "buildings_by_campus", BuildingsByCampus._TYPE,
				BuildingsByCampusService);

		/***************************************************
		 * BuildingsByBuildingCode Service Define BuildingsByBuildingCode and it's
		 * callback
		 ****************************************************/
		ServiceResponseBuilder<BuildingsByBuildingCodeRequest, BuildingsByBuildingCodeResponse> BuildingsByBuildingCodeService = new ServiceResponseBuilder<BuildingsByBuildingCodeRequest, BuildingsByBuildingCodeResponse>() {
			@Override
			public void build(BuildingsByBuildingCodeRequest request, BuildingsByBuildingCodeResponse response) {
				String bucode = request.getBucode();
				if (bucode.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.buildingsByBuildingCode(bucode);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "buildings_by_building_code", BuildingsByBuildingCode._TYPE,
				BuildingsByBuildingCodeService);

		/***************************************************
		 * NearbyBuildings Service Define NearbyBuildings and it's callback
		 ****************************************************/
		ServiceResponseBuilder<NearbyBuildingsRequest, NearbyBuildingsResponse> NearbyBuildingsService = new ServiceResponseBuilder<NearbyBuildingsRequest, NearbyBuildingsResponse>() {
			@Override
			public void build(NearbyBuildingsRequest request, NearbyBuildingsResponse response) {
				String coordinates_lat = request.getCoordinatesLat();
				String coordinates_lon = request.getCoordinatesLon();
				if (coordinates_lat.isEmpty() || coordinates_lon.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.nearbyBuildings(access_token, coordinates_lat, coordinates_lon);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "nearby_buildings", NearbyBuildings._TYPE,
				NearbyBuildingsService);

		/***************************************************
		 * AllBuildingFloors Service Define AllBuildingFloors and it's callback
		 ****************************************************/
		ServiceResponseBuilder<BuildingServiceRequest, BuildingServiceResponse> AllBuildingFloorsService = new ServiceResponseBuilder<BuildingServiceRequest, BuildingServiceResponse>() {
			@Override
			public void build(BuildingServiceRequest request, BuildingServiceResponse response) {
				String buid = request.getBuid();
				if (buid.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.allBuildingFloors(buid);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "all_building_floors", BuildingService._TYPE,
				AllBuildingFloorsService);

		/***************************************************
		 * AllBuildingPOIs Service Define AllBuildingPOIs and it's callback
		 ****************************************************/
		ServiceResponseBuilder<BuildingServiceRequest, BuildingServiceResponse> AllBuildingPOIsService = new ServiceResponseBuilder<BuildingServiceRequest, BuildingServiceResponse>() {
			@Override
			public void build(BuildingServiceRequest request, BuildingServiceResponse response) {
				String buid = request.getBuid();
				if (buid.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.allBuildingPOIs(buid);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "all_building_pois", BuildingService._TYPE,
				AllBuildingPOIsService);

		/***************************************************
		 * AllBuildingFloorPOIs Service Define AllBuildingFloorPOIs and it's callback
		 ****************************************************/
		ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse> AllBuildingFloorPOIsService = new ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse>() {
			@Override
			public void build(FloorServiceRequest request, FloorServiceResponse response) {
				String buid = request.getBuid();
				String floor = request.getFloor();
				if (buid.isEmpty() || floor.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.allBuildingFloorPOIs(buid, floor);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "all_building_floor_pois", FloorService._TYPE,
				AllBuildingFloorPOIsService);

		/***************************************************
		 * ConnectionsByFloor Service Define ConnectionsByFloor and it's callback
		 ****************************************************/
		ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse> ConnectionsByFloorService = new ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse>() {
			@Override
			public void build(FloorServiceRequest request, FloorServiceResponse response) {
				String buid = request.getBuid();
				String floor = request.getFloor();
				if (buid.isEmpty() || floor.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.connectionsByFloor(buid, floor);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "connections_by_floor", FloorService._TYPE,
				ConnectionsByFloorService);

		/***************************************************
		 * HeatmapBuidFloor Service Define HeatmapBuidFloor and it's callback
		 ****************************************************/
		ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse> HeatmapBuidFloorService = new ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse>() {
			@Override
			public void build(FloorServiceRequest request, FloorServiceResponse response) {
				String buid = request.getBuid();
				String floor = request.getFloor();
				if (buid.isEmpty() || floor.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.radioheatMapBuildingFloor(buid, floor);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "heatmap_buid_floor", FloorService._TYPE,
				HeatmapBuidFloorService);

		/***************************************************
		 * Floor64 Service Define Floor64 and it's callback
		 ****************************************************/
		ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse> Floor64Service = new ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse>() {
			@Override
			public void build(FloorServiceRequest request, FloorServiceResponse response) {
				String buid = request.getBuid();
				String floor = request.getFloor();
				if (buid.isEmpty() || floor.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.floorplans64(access_token, buid, floor);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "floor_64", FloorService._TYPE, Floor64Service);

		/***************************************************
		 * Floortiles Service Define Floortiles and it's callback
		 ****************************************************/
		ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse> FloortilesService = new ServiceResponseBuilder<FloorServiceRequest, FloorServiceResponse>() {
			@Override
			public void build(FloorServiceRequest request, FloorServiceResponse response) {
				String buid = request.getBuid();
				String floor = request.getFloor();
				if (buid.isEmpty() || floor.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.floortiles(access_token, buid, floor);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "floortiles", FloorService._TYPE, FloortilesService);

		/***************************************************
		 * RadioByCoordinatesFloor Service Define RadioByCoordinatesFloor and it's
		 * callback
		 ****************************************************/
		ServiceResponseBuilder<RadioByCoordinatesFloorRequest, RadioByCoordinatesFloorResponse> RadioByCoordinatesFloorService = new ServiceResponseBuilder<RadioByCoordinatesFloorRequest, RadioByCoordinatesFloorResponse>() {
			@Override
			public void build(RadioByCoordinatesFloorRequest request, RadioByCoordinatesFloorResponse response) {
				String coordinates_lat = request.getCoordinatesLat();
				String coordinates_lon = request.getCoordinatesLon();
				String floor = request.getFloor();
				if (coordinates_lat.isEmpty() || coordinates_lon.isEmpty() || floor.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				String anyplace_response = client.radioByCoordinatesFloor(access_token, coordinates_lat,
						coordinates_lon, floor);
				connectedNode.getLog().info(anyplace_response + "\n");
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "radio_by_coordinates_floor", RadioByCoordinatesFloor._TYPE,
				RadioByCoordinatesFloorService);


		/***************************************************
		 * EstimatePosition Service Define EstimatePositionService and it's callback
		 ****************************************************/
		ServiceResponseBuilder<EstimatePositionRequest, EstimatePositionResponse> EstimatePositionService = new ServiceResponseBuilder<EstimatePositionRequest, EstimatePositionResponse>() {
			@Override
			public void build(EstimatePositionRequest request, EstimatePositionResponse response) {

				String operating_system = request.getOperatingSystem();
				String buid = request.getBuid();
				String floor = request.getFloor();
				String algorithm = request.getAlgorithm();

				if (operating_system.isEmpty() || buid.isEmpty() || floor.isEmpty() || algorithm.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				if (debug == true) {
					connectedNode.getLog().info("Got request, with info:\n os: " + operating_system + ",\n buid: "
							+ buid + ",\n floor: " + floor + ",\n algorithm: " + algorithm + "\n");
				}
				String cmd[] = new String[3];
				if (operating_system.equals("linux")) {
					cmd[0] = "/bin/bash";
					cmd[1] = "-c";
					cmd[2] = "iwlist wlo1 scan | awk  '/Address/ {print $5}; /level/ {print $3}' |  cut -d\"=\" -f2 ";
					connectedNode.getLog().info("Cmd on Linux is: " + cmd[0] +" "+ cmd[1] +" "+ cmd[2] + "\n");
				} else if (operating_system.equals("mac")) {
					cmd[0] = "/bin/sh";
					cmd[1] = "-c";
					cmd[2] = "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport -s | grep ':' | tr -s ' ' | cut -d' ' -f3 -f4| tr ' ' '\n'";
					connectedNode.getLog().info("Cmd on Mac is: " + cmd[0] +" "+ cmd[1] +" "+ cmd[2] + "\n");
				} else {
					response.setSuccess(false);
					response.setResponse("Only linux and Mac are the available operating systems\n Returning...");
					return;
				}

				String aps[] = new String[200];
				Process p;
				String s, temp;
				int counter = 0;
				try {
					p = Runtime.getRuntime().exec(cmd);

					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((s = br.readLine()) != null && counter <= 20) {
						temp = "{\"bssid\":\"";
						temp += s;
						temp += "\",\"rss\":";
						s = br.readLine();
						temp += s;
						temp += "}";
						temp = temp.toLowerCase();
						aps[counter++] = temp;
					}
					p.destroy();
					br.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				aps = Arrays.copyOf(aps, counter);
				for (int j = 0; j < counter; j++) {
					connectedNode.getLog().info(aps[j] + "\n");
				}
				String anyplace_response = client.estimatePosition(buid, floor, aps, algorithm);
				connectedNode.getLog().info(anyplace_response + "\n"); /* .substring(0, 100) */

				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "estimate_position", EstimatePosition._TYPE,
				EstimatePositionService);

		/***************************************************
		 * EstimatePositionOffline Service Define EstimatePositionOfflineService and
		 * it's callback
		 ****************************************************/
		ServiceResponseBuilder<EstimatePositionRequest, EstimatePositionResponse> EstimatePositionOfflineService = new ServiceResponseBuilder<EstimatePositionRequest, EstimatePositionResponse>() {
			@Override
			public void build(EstimatePositionRequest request, EstimatePositionResponse response) {

				String operating_system = request.getOperatingSystem();
				String buid = request.getBuid();
				String floor = request.getFloor();
				String algorithm = request.getAlgorithm();

				if (operating_system.isEmpty() || buid.isEmpty() || floor.isEmpty() || algorithm.isEmpty()) {
					response.setSuccess(false);
					response.setResponse("Service parameters cannot be empty!\n Returning...");
					return;
				}
				if (debug == true) {
					connectedNode.getLog().info("Got request, with info:\n os: " + operating_system + ",\n buid: "
							+ buid + ",\n floor: " + floor + ",\n algorithm: " + algorithm + "\n");
				}
				String cmd[] = new String[3];
				if (operating_system.equals("linux")) {
					cmd[0] = "/bin/sh";
					cmd[1] = "-c";
					cmd[2] = "sudo iwlist wlo1 scan | awk  '/Address/ {print $5}; /level/ {print $3}' |  cut -d\"=\" -f2 ";

				} else if (operating_system.equals("mac")) {
					cmd[0] = "/bin/sh";
					cmd[1] = "-c";
					cmd[2] = "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport -s | grep ':' | tr -s ' ' | cut -d' ' -f3 -f4| tr ' ' '\n'";

				} else {
					response.setSuccess(false);
					response.setResponse("Only linux and mac are the available operating systems\n Returning...");
					return;
				}

				String aps[] = new String[200];
				Process p;
				String s, temp;
				int counter = 0;
				try {
					p = Runtime.getRuntime().exec(cmd);

					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((s = br.readLine()) != null && counter <= 20) {
						temp = "{\"bssid\":\"";
						temp += s;
						temp += "\",\"rss\":";
						s = br.readLine();
						temp += s;
						temp += "}";
						temp = temp.toLowerCase();
						aps[counter++] = temp;
					}
					p.destroy();
					br.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				aps = Arrays.copyOf(aps, counter);

				String anyplace_response = client.estimatePositionOffline(buid, floor, aps, algorithm);
				connectedNode.getLog().info(anyplace_response + "\n"); /* .substring(0, 100) */
				response.setSuccess(true);
				response.setResponse(anyplace_response);
			}
		};
		connectedNode.newServiceServer(root_namespace + "estimate_position_offline", EstimatePosition._TYPE,
				EstimatePositionOfflineService);
	}

}
