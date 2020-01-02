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

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import rosjava_custom_srv.CustomServiceResponse;
import rosjava_custom_srv.CustomServiceRequest;
import rosjava_custom_srv.CustomService;

/**
 * A simple {@link ServiceClient} {@link NodeMain}.
   The original code is created by:
 *
 * @author damonkohler@google.com (Damon Kohler)
 * The custom implementation is created by
   v.s.moisiadis@gmail.com(Vasileios Moisiadis)
 */
public class Client extends AbstractNodeMain {

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("rosjava_tutorial_custom_custom_services/client");
  }

  @Override
  public void onStart(final ConnectedNode connectedNode) {
    ServiceClient<CustomServiceRequest, CustomServiceResponse> serviceClient;
    try {
      serviceClient = connectedNode.newServiceClient("CustomService", CustomService._TYPE);
    } catch (ServiceNotFoundException e) {
      throw new RosRuntimeException(e);
    }
    final CustomServiceRequest request = serviceClient.newMessage();
    //set the request/size

    request.setSize(10);

    serviceClient.call(request, new ServiceResponseListener<CustomServiceResponse>() {
      @Override
      public void onSuccess(CustomServiceResponse response) {
        connectedNode.getLog().info(
                String.format("The response is : "));

        for (long l : response.getRes()) {
          connectedNode.getLog().info(l);
        }
      }

      @Override
      public void onFailure(RemoteException e) {
        throw new RosRuntimeException(e);
      }
    });
  }
}