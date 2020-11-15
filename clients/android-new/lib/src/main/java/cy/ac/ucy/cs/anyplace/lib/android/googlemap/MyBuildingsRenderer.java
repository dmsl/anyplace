/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Timotheos Constambeys
* 
* Supervisor: Demetrios Zeinalipour-Yazti
*
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
* All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package cy.ac.ucy.cs.anyplace.lib.android.googlemap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import cy.ac.ucy.cs.anyplace.lib.R;
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel;

public class MyBuildingsRenderer extends DefaultClusterRenderer <BuildingModel>{

    private Context ctx;

	public MyBuildingsRenderer(Context context, GoogleMap map, ClusterManager<BuildingModel> clusterManager) {
		super(context, map, clusterManager);
		ctx = context;
	}
	
    @Override
    protected void onBeforeClusterItemRendered(BuildingModel bm, MarkerOptions markerOptions) {

       // markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin2));

       // markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.building_icon));
       markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeBuildingIcon(128,128)));

      // markerOptions.icon(BitmapDescriptorFactory.fromFile("pin2.png"));
    }

  public Bitmap resizeBuildingIcon( int width, int height){
    // Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
    Bitmap imageBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.building_icon);
    Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    return resizedBitmap;
  }

}
