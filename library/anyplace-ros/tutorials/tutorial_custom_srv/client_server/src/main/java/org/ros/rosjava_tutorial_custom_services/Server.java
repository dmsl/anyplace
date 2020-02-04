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

package org.ros.rosjava_tutorial_custom_services;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.node.service.ServiceServer;
import java.io.Console;
import rosjava_custom_srv.CustomService;
import rosjava_custom_srv.CustomServiceRequest;
import rosjava_custom_srv.CustomServiceResponse;
/**
 * A simple {@link ServiceServer} {@link NodeMain}.
   The original code is created by:
 *
 * @author damonkohler@google.com (Damon Kohler)
 * The custom implementation is created by
   v.s.moisiadis@gmail.com(Vasileios Moisiadis)
 */
public class Server extends AbstractNodeMain {

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("rosjava_tutorial_custom_services/server");
  }

  @Override
  public void onStart(final ConnectedNode connectedNode) {
    connectedNode.newServiceServer("CustomService", CustomService._TYPE,
        new ServiceResponseBuilder<CustomServiceRequest, CustomServiceResponse>() {
          @Override
          public void
              build(CustomServiceRequest request, CustomServiceResponse response) {
              //Create an array with the size of request.getSize()
              long[] tmpArray=new long[request.getSize()];
              for(int i=0; i<request.getSize();i++){
                  tmpArray[i]=i;
              }
               response.setRes(tmpArray);
                   connectedNode.getLog().info(
                           String.format("The size of the array will be "+request.getSize()));

          }
        });
  }
}