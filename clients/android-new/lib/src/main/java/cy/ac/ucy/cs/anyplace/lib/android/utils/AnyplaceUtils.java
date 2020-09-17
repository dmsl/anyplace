/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Lambros Petrou, Timotheos Constambeys
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

package cy.ac.ucy.cs.anyplace.lib.android.utils;

import java.io.File;

import android.content.Context;

public class AnyplaceUtils {

	public static File getFolderRootFolder(Context ctx, String folder) throws Exception {
		if (!AndroidUtils.checkExternalStorageState()) {
			throw new Exception("Error: It seems that we cannot write on your sdcard!");
		}

		File sdcard_root = ctx.getExternalFilesDir(null);
		if (sdcard_root == null) {
			throw new Exception("Error: It seems we cannot access the sdcard!");
		}
		File root = new File(sdcard_root, folder);
		root.mkdirs();
		if (root.isDirectory() == false) {
			throw new Exception("Error: It seems we cannot write on the sdcard!");
		}
		return root;
	}

	/**
	 * Returns the File to the root folder where floor plans are stored on the
	 * device external memory
	 * 
	 * @return
	 * @throws Exception
	 */
	public static File getFloorPlansRootFolder(Context ctx) throws Exception {
		return getFolderRootFolder(ctx, "floor_plans");
	}

	/**
	 * Returns the File to the root folder where radio maps are stored on the
	 * device external memory
	 * 
	 * @return
	 * @throws Exception
	 */
	public static File getRadioMapsRootFolder(Context ctx) throws Exception {
		return getFolderRootFolder(ctx, "radiomaps");
	}

	/**
	 * Returns the filename for the radiomap to be used according to the floor
	 * selected
	 * 
	 * @return
	 */
	public static String getRadioMapFileName(String floor) {
		return "fl_" + (floor == null ? "-" : floor) + "_indoor-radiomap.txt";
	}

	public static File getRadioMapFoler(Context ctx, String buid, String floor) throws Exception {
		File root = getRadioMapsRootFolder(ctx);
		File file = new File(root, (buid == null ? "-" : buid) + "fl_" + (floor == null ? "-" : floor));
		file.mkdirs();

		if (file.isDirectory() == false) {
			throw new Exception("Error: It seems we cannot write on the sdcard!");
		}

		return file;
	}

}
