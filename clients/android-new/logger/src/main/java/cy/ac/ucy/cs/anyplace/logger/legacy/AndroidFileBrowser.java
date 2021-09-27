/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Lambros Petrou
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

package cy.ac.ucy.cs.anyplace.logger.legacy;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;



import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import cy.ac.ucy.cs.anyplace.logger.R;

public class AndroidFileBrowser extends ListActivity implements OnClickListener {
  private static final String TAG = AndroidFileBrowser.class.getSimpleName() ;

  /*
	 * Select File Code Intent i = new Intent(getBaseContext(),
	 * AndroidFileBrowser.class); Bundle extras = new Bundle();
	 * extras.putBoolean("selectFolder", false); extras.putString("defaultPath",
	 * preferences.getString("upload_file", "")); i.putExtras(extras);
	 * startActivityForResult(i, UPLOAD_FILE);
	 */
	/*
	 * 
	 * case UPLOAD_FILE: if (resultCode == Activity.RESULT_OK) { Uri
	 * selectedFile = data.getData(); String file = selectedFile.toString();
	 * SharedPreferences.Editor editor = preferences.edit();
	 * editor.putString("upload_file", file); editor.commit();
	 * startUploadTask(file); } break; }
	 */

	// Enum For The Display Mode You Want
	private enum DISPLAYMODE {
		ABSOLUTE, RELATIVE;
	}

	private boolean selectFolder = true;

	private final DISPLAYMODE displayMode = DISPLAYMODE.ABSOLUTE;
	private Button button_select;

	private File homeDirectory = new File("/");
	private List<String> directoryEntries = new ArrayList<String>();

	private File currentDirectory;
	private TextView pwd;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setContentView(R.layout.main_choose_file_or_directory);

		pwd = (TextView) findViewById(R.id.pwd);
		button_select = (Button) findViewById(R.id.select_file_folder);
		button_select.setOnClickListener(this);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			selectFolder = extras.getBoolean("selectFolder");
		}

		if (selectFolder)
			button_select.setText("Save in this folder");
		else {
			button_select.setText("Select Upload File");
		}

		String defaultPath = extras.getString("defaultPath");
		File defaultFile = new File(checkPath(defaultPath));

		if (defaultFile.isDirectory())
			currentDirectory = defaultFile;
		else {
			pwd.setText(defaultFile.getAbsolutePath());
			currentDirectory = defaultFile.getParentFile();
		}
		browseTo(currentDirectory);

	}

	// check that the sdcard is mounted
	static public boolean isMount() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	private String checkPath(String path) {
		if (path == null || path.length() == 0) {
			return "/";
		}

		File check = new File(path);
		if (!check.exists() || !check.canRead()) {
			return "/";
		}
		return path;
	}

	private void browseTo(final File aDirectory) {
		if (aDirectory.isDirectory()) {
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());

			if (selectFolder)
				pwd.setText(currentDirectory.getAbsolutePath());
		}
	}

	private void fill(File[] files) {
		Arrays.sort(files, new SortIgnoreCase());
		this.directoryEntries.clear();
		// Add the "~" == "home directory"
		// And the ".." == 'Up one level'
		this.directoryEntries.add(getString(R.string.homeDir));
		if (this.currentDirectory.getParent() != null)
			this.directoryEntries.add(getString(R.string.parentDir));

		switch (this.displayMode) {

		case ABSOLUTE:
			for (File file : files) {
				this.directoryEntries.add(file.getAbsolutePath());
			}
			break;
		case RELATIVE: // On relative Mode, we have to add the current-path to
			// the beginning
			int currentPathStringLenght = this.currentDirectory.getAbsolutePath().length();
			for (File file : files) {
				this.directoryEntries.add(file.getAbsolutePath().substring(currentPathStringLenght));
			}
			break;
		}

		MyCustomAdapter directoryList = new MyCustomAdapter(this, R.layout.file_row, this.directoryEntries);

		this.setListAdapter(directoryList);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		File file = new File(directoryEntries.get(position));

		if (file.isDirectory()) {
			if (file.canRead()) {
				if (file.getName().equals(getString(R.string.parentDir))) {
					browseTo(currentDirectory.getParentFile());
				} else if (file.getName().equals(getString(R.string.homeDir))) {
					browseTo(homeDirectory);
					Log.e(TAG, "Browsing home directory in AndroidFileBrowser" );
				} else
					browseTo(file);
			} else {
				showAlert("Read Permission Denied", "Warning", this);
			}
		} else if (!file.isDirectory() && selectFolder) {
			showAlert("Not a directory", "Warning", this);
		} else {
			pwd.setText(file.getAbsolutePath());
		}
		super.onListItemClick(l, v, position, id);

	}

	public static void showAlert(String message, String title, Context ctx) {
		// Create a builder
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(title);
		// add buttons and listener

		builder.setMessage(message).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		// Create the dialog
		AlertDialog ad = builder.create();
		// show
		ad.show();
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.select_file_folder:

			if (selectFolder) {

				if (currentDirectory.canWrite()) {
					Intent data = new Intent();
					data.setData(Uri.parse(currentDirectory.getAbsolutePath()));
					setResult(RESULT_OK, data);
					finish();
				} else
					showAlert("Write Permission Denied", "Warning", this);

			} else {
				File file = new File(pwd.getText().toString());
				if (file.exists()) {
					if (file.canRead()) {

						Intent data = new Intent();
						data.setData(Uri.parse(pwd.getText().toString()));
						setResult(RESULT_OK, data);
						this.finish();

					} else
						showAlert("Read Permission Denied", "Warning", this);
				} else
					showAlert("No file selected", "Warning", this);
				break;
			}
		}
	}

	/**
	 * Control when back button is pressed
	 * */
	@Override
	public void onBackPressed() {
		finish();
	}

	// Public Inner Class Which Help Me To Put Png Image For Each File That Is
	// Different Type
	class MyCustomAdapter extends ArrayAdapter<String> {
		List<String> myList;

		public MyCustomAdapter(Context context, int textViewResourceId, List<String> objects) {

			super(context, textViewResourceId, objects);
			myList = objects;
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			// super.getView(position, convertView, parent);
			View row = convertView;
			// Check If Row Is Null
			if (row == null) {
				// Make New Layoutinflater
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = vi.inflate(R.layout.file_row, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.file);
			String stringFile = myList.get(position).toString();
			// Change The Symbol Of The Home Directory
			if (stringFile.equals(getString(R.string.homeDir)))
				label.setText("~");
			else
				label.setText(stringFile);

			File file = new File(stringFile);
			ImageView icon = (ImageView) row.findViewById(R.id.icon);

			if (file.isDirectory())
				icon.setImageResource(R.drawable.directory);
			else
				icon.setImageResource(FindDrawable(stringFile));
			return row;
		}

		// Find The Right Icon For Each File Type
		private int FindDrawable(String file) {
			if (file.endsWith(".txt")) {
				return R.drawable.txt;
			} else if (file.endsWith(".pdf")) {
				return R.drawable.pdf;
			} else if (file.endsWith(".exe")) {
				return R.drawable.exe;
			} else if (file.endsWith(".apk")) {
				return R.drawable.apk;
			} else if (file.endsWith(".png")) {
				return R.drawable.png;
			} else if (file.endsWith(".gif")) {
				return R.drawable.gif;
			} else if (file.endsWith(".jpg")) {
				return R.drawable.jpg;
			} else if (file.endsWith(".rar")) {
				return R.drawable.rar;
			} else if (file.endsWith(".zip")) {
				return R.drawable.zip;
			} else if (file.endsWith(".gz")) {
				return R.drawable.gz;
			} else if (file.endsWith(".mp3") || file.endsWith(".wav") || file.endsWith(".amp")) {
				return R.drawable.sound;
			} else if (file.endsWith(".mp4") || file.endsWith(".avi") || file.endsWith(".flv")) {
				return R.drawable.video;
			} else
				return R.drawable.txt;
		}
	}

	static class SortIgnoreCase implements Comparator<Object> {
		public int compare(Object o1, Object o2) {

			return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
		}
	}
}
