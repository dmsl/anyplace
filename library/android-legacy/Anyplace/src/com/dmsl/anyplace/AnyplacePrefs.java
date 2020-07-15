/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
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

package com.dmsl.anyplace;

import java.io.File;

import com.dmsl.anyplace.tasks.DeleteFolderBackgroundTask;
import com.dmsl.anyplace.utils.AnyplaceUtils;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

/**
 * Defines the behavior of the preferences menu.
 * 
 * @author KIOS Research Center and Data Management Systems Lab, University of Cyprus
 *
 */
public class AnyplacePrefs extends PreferenceActivity {

	public enum Action {
		REFRESH_BUILDING, REFRESH_MAP
	}

	/**
	 * Build preference menu when the activity is first created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Load the appropriate preferences
		getPreferenceManager().setSharedPreferencesName(UnifiedNavigationActivity.SHARED_PREFS_ANYPLACE);

		addPreferencesFromResource(R.xml.preferences_anyplace);

		getPreferenceManager().findPreference("clear_radiomaps").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				File root;
				try {
					root = AnyplaceUtils.getRadioMapsRootFolder(AnyplacePrefs.this);
					DeleteFolderBackgroundTask task = new DeleteFolderBackgroundTask(AnyplacePrefs.this);
					task.setFiles(root);
					task.execute();
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
				}
				return true;
			}
		});

		getPreferenceManager().findPreference("clear_floorplans").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				File root;
				try {
					root = AnyplaceUtils.getFloorPlansRootFolder(AnyplacePrefs.this);
					DeleteFolderBackgroundTask task = new DeleteFolderBackgroundTask(AnyplacePrefs.this);
					task.setFiles(root);
					task.execute();
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
				}

				return true;
			}
		});

		getPreferenceManager().findPreference("refresh_building").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("action", Action.REFRESH_BUILDING);
				setResult(RESULT_OK, returnIntent);
				finish();
				return true;
			}
		});
		getPreferenceManager().findPreference("refresh_map").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("action", Action.REFRESH_MAP);
				setResult(RESULT_OK, returnIntent);
				finish();
				return true;
			}
		});

		// Customize the description of algorithms
		getPreferenceManager().findPreference("Short_Desc").setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				final String[] names = getResources().getStringArray(R.array.AlgorithmsNames);
				final String[] descriptions = getResources().getStringArray(R.array.AlgorithmsDescriptions);

				// TODO Auto-generated method stub
				AlertDialog.Builder builder = new AlertDialog.Builder(AnyplacePrefs.this);

				builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						// Show something if does not exit the app
						dialog.dismiss();
					}
				});

				builder.setTitle("Algorithms Short Description");
				builder.setSingleChoiceItems(names, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switch (item) {
						case 0:
							popup_msg(descriptions[0], names[0], 0);
							break;
						case 1:
							popup_msg(descriptions[1], names[1], 0);
							break;
						case 2:
							popup_msg(descriptions[2], names[2], 0);
							break;
						case 3:
							popup_msg(descriptions[3], names[3], 0);
							break;
						}

					}
				});

				AlertDialog alert = builder.create();

				alert.show();
				return true;

			}
		});
	}

	/**
	 * Actions to be taken on Preference menu exit.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		default:
			break;
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void popup_msg(String msg, String title, int imageID) {

		AlertDialog.Builder alert_box = new AlertDialog.Builder(this);
		alert_box.setTitle(title);
		alert_box.setMessage(msg);
		alert_box.setIcon(imageID);

		alert_box.setNeutralButton("Hide", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		AlertDialog alert = alert_box.create();
		alert.show();
	}

}