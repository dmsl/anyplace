/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Timotheos Constambeys, Lambros Petrou
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;
import java.util.HashMap;
import java.util.ArrayList;


import android.content.Context;
import android.util.Log;
import android.util.SparseArray;


import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import cy.ac.ucy.cs.anyplace.lib.android.utils.AnyplaceUtils;

public class MapTileProvider implements TileProvider {

    private static final boolean DEBUG = true;
    private static final String TAG = MapTileProvider.class.getSimpleName();

	private static final int TILE_WIDTH = 256;
	private static final int TILE_HEIGHT = 256;
	private static final int BUFFER_SIZE = 16 * 1024;

	private Context mCtx;
	private String dirPath;
	private SparseArray<Integer[]> mTileBounds;

	public MapTileProvider(Context ctx, String buid, String floor_number) {
		this.mCtx = ctx;
		this.dirPath = buid + File.separatorChar + floor_number + File.separatorChar + "tiles_archive" + File.separatorChar;

		readBounds();
	}

	@Override
	public Tile getTile(int x, int y, int zoom) {
		Tile result;
		byte[] image = readTileImage(x, y, zoom);
		if (image == null) {
			result = NO_TILE;
		} else {
			result = new Tile(TILE_WIDTH, TILE_HEIGHT, image);
		}
		return result;
	}

	private byte[] readTileImage(int x, int y, int zoom) {
		if (mTileBounds != null) {
			Integer[] bounds = mTileBounds.get(zoom);
			if (bounds == null) {
				return null;
			}
			if (x < bounds[0] || x > bounds[1] || y < bounds[2] || y > bounds[3]) {
				return null;
			}
		}

		FileInputStream in = null;
		ByteArrayOutputStream buffer = null;

		try {
			File tileFile = getTileFile(x, y, zoom);

			if (!tileFile.exists() || !tileFile.canRead()) {
				Log.d("tiler-response", "Not found: " + tileFile.getAbsolutePath());
				return null;
			}
			in = new FileInputStream(tileFile);
			buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[BUFFER_SIZE];

			while ((nRead = in.read(data, 0, BUFFER_SIZE)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();

			return buffer.toByteArray();
		} catch (IOException e) {
			Log.d("AnyplaceTileProvider", e.getMessage());
			return null;
		} catch (OutOfMemoryError e) {
			Log.d("AnyplaceTileProvider", e.getMessage());
			return null;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (Exception ignored) {
					Log.d("AnyplaceIO", "Cannot close input file of Tile!");
				}
			if (buffer != null)
				try {
					buffer.close();
				} catch (Exception ignored) {
					Log.d("AnyplaceIO", "Cannot close buffer file of Tile!");
				}
		}
	}

	private File getTileFile(int x, int y, int zoom) {
		File rootFloorsDir;
		try {
			rootFloorsDir = AnyplaceUtils.getFloorPlansRootFolder(mCtx);
		} catch (Exception e) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(dirPath);
		sb.append(zoom);
		sb.append(File.separatorChar + "z" + zoom);
		sb.append("x" + x);
		sb.append("y" + y);
		sb.append(".png");

		File file = new File(rootFloorsDir, sb.toString());
		return file;
	}

	private void readBounds() {
		File rootFloorsDir;
		try {
			rootFloorsDir = AnyplaceUtils.getFloorPlansRootFolder(mCtx);
		} catch (Exception e) {
			Log.d("TileProvider", "Cannot get sdcard read access!");
			return;
		}
		File boundsFile = new File(rootFloorsDir, dirPath + "bounds.txt");
		if (!boundsFile.exists()) {
			Log.d("TileProvider", "bounds.txt does not exist!");
			return;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(boundsFile));
			mTileBounds = new SparseArray<Integer[]>();
			String line;
			String[] segs;
			while ((line = reader.readLine()) != null) {
				if (!line.matches("^[0-9]+,[0-9]+,[0-9]+,[0-9]+,[0-9]+$")) {
					continue;
				}

				segs = line.split(",");
				// I add TILE_WIDTH and TILE_HEIGHT at each dimension because we
				// might have some
				// differences on the tile number requested.
				// I tried with diff=1 but some tiles were blocked
				mTileBounds.put(Integer.parseInt(segs[0]), new Integer[] { Integer.parseInt(segs[1]), Integer.parseInt(segs[2]), Integer.parseInt(segs[3]), Integer.parseInt(segs[4]) });
			}

			reader.close();

		} catch (FileNotFoundException e) {
			mTileBounds = null;
			Log.d("TileProvider", "Error while reading bounds.txt!" + e.getMessage());
			return;
		} catch (IOException e) {
			mTileBounds = null;
			Log.d("TileProvider", "IOException while reading bounds.txt!" + e.getMessage());
			return;
		}

	}
  public static Collection<WeightedLatLng> readRadioMapLocations(File inFile) {
    class Weight {
      String lat;
      String lot;
      int intesity;
    }
    //sixth decimal place is worth up to 0.11 m
    final int decimal_place = 6;
    HashMap<String, Weight> locations = new HashMap<String, Weight>();
    BufferedReader reader = null;
    String line = null;
    String[] temp = null;
    String key = null;
    try {

      reader = new BufferedReader(new FileReader(inFile));

      // Read the first line # NaN -110
      line = reader.readLine();
      temp = line.split(" ");
      if (!temp[1].equals("NaN")) {
        if (DEBUG) {
          Log.d(TAG, "readMapLocations first one is not NaN");
        }
        return null;
      }

      line = reader.readLine();

      // Must exists
      if (line == null) {
        if(DEBUG){
          Log.d(TAG, "readMapLocations second line is empty" );
        }
        return null;
      }
      line = line.replace(", ", " ");
      temp = line.split(" ");

      final int startOfRSS = 4;

      // Must have more than 4 fields
      if (temp.length < startOfRSS) {
        if(DEBUG){
          Log.d(TAG, "readMapLocations lines have less than 4 fields" );
        }
        return null;
      }

      while ((line = reader.readLine()) != null) {

        if (line.trim().equals(""))
          continue;

        line = line.replace(", ", " ");
        temp = line.split(" ");

        if (temp.length < startOfRSS){
          if(DEBUG){
            Log.d(TAG, "readMapLocations lines have less than 4 fields" );
          }
          return null;
        }
        String lat;
        String lot;
        try {
          lat = temp[0].substring(0, temp[0].indexOf(".") + decimal_place);
        } catch (IndexOutOfBoundsException e) {
          lat = temp[0];
        }

        try {
          lot = temp[1].substring(0, temp[1].indexOf(".") + decimal_place);
        } catch (IndexOutOfBoundsException e) {
          lot = temp[1];
        }

        key = lat + " " + lot;
        Weight weight = locations.get(key);
        if (weight == null) {
          weight = new Weight();
          weight.lat = temp[0];
          weight.lot = temp[1];
          locations.put(key, weight);
        }

        weight.intesity++;
      }

      Collection<WeightedLatLng> collection = new ArrayList<WeightedLatLng>();
      for (Weight w : locations.values()) {
        collection.add(new WeightedLatLng(new LatLng(Double.parseDouble(w.lat), Double.parseDouble(w.lot)), w.intesity));
      }
      if(DEBUG){
        Log.d(TAG, "readMapLocations collection retrieved successfully" );
      }

      return collection;
    } catch (Exception ex) {

      if(DEBUG){
        Log.d(TAG, "readMapLocations exception with " + ex.getMessage() );
      }

      return null;
    } finally {
      if (reader != null)
        try {
          reader.close();
        } catch (IOException e) {

        }
    }
  }


}
