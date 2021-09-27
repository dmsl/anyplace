/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
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

package cy.ac.ucy.cs.anyplace.logger.legacy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.FragmentActivity;

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp;
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper;
import cy.ac.ucy.cs.anyplace.lib.android.nav.IPoisClass;
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper.SearchTypes;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.AnyplaceSuggestionsTask;
import cy.ac.ucy.cs.anyplace.lib.android.utils.AndroidUtils;
import cy.ac.ucy.cs.anyplace.lib.android.utils.GeoPoint;
import cy.ac.ucy.cs.anyplace.logger.R;

public class SearchPOIActivity extends FragmentActivity {

  private TextView txtResultsFound;
  private ListView lvResultPois;

  private List<Spanned> mQueriedPoisStr;
  private List<? extends IPoisClass> mQueriedPois;

  private AnyPlaceSeachingHelper.SearchTypes mSearchType;

  private AnyplaceApp app;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    app = (AnyplaceApp) getApplication();

    setTitle("Points of Interest");
    setContentView(R.layout.activity_search_poi);

    txtResultsFound = (TextView) findViewById(R.id.txtResultsFound);

    lvResultPois = (ListView) findViewById(R.id.lvResultPois);
    lvResultPois.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
          // handle the case of Google Place
          IPoisClass place = null;
          if (mSearchType == SearchTypes.INDOOR_MODE) {
            place = mQueriedPois.get(position);
          } else if (mSearchType == SearchTypes.OUTDOOR_MODE) {
            place = mQueriedPois.get(position);
          }
          finishSearch("Success!", place);
        } catch (ArrayIndexOutOfBoundsException e) {
          Toast.makeText(getApplicationContext(), "Something went wrong with your selection!", Toast.LENGTH_LONG).show();
          finish();
        }
      }
    });

    handleIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handleIntent(intent);
  }

  private void handleIntent(Intent intent) {
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      // get the search type
      mSearchType = (SearchTypes) intent.getSerializableExtra("searchType");
      if (mSearchType == null)
        finishSearch("No search type provided!", null);

      // get the query string
      final String query = intent.getStringExtra("query");
      double lat = intent.getDoubleExtra("lat", 0);
      double lng = intent.getDoubleExtra("lng", 0);
      String key = getString(R.string.maps_api_key);
      AnyplaceSuggestionsTask mSuggestionsTask = new AnyplaceSuggestionsTask(
              app,
              new AnyplaceSuggestionsTask.AnyplaceSuggestionsListener() {

                @Override
                public void onSuccess(String result, List<? extends IPoisClass> pois) {

                  // we have pois to query for a match
                  mQueriedPoisStr = new ArrayList<Spanned>();
                  mQueriedPois = pois;

                  // Display part of Description Text Only
                  // Make an approximation of available space based on map size
                  final int viewWidth = (int) (findViewById(R.id.txtResultsFound).getWidth() * 2);
                  View infoWindow = getLayoutInflater().inflate(R.layout.queried_pois_item_1_searchactivity, null);
                  TextView infoSnippet = (TextView) infoWindow;
                  TextPaint paint = infoSnippet.getPaint();

                  // Regular expression
                  // ?i ignore case
                  Pattern pattern = Pattern.compile(String.format("((?i)%s)", query));

                  for (IPoisClass pm : pois) {
                    String name = "", description = "";
                    Matcher m;
                    m = pattern.matcher(pm.name());
                    // Makes matched query bold using HTML format
                    // $1 returns the regular's expression outer parenthesis value
                    name = m.replaceAll("<b>$1</b>");

                    m = pattern.matcher(pm.description());
                    if (m.find()) {
                      // Makes matched query bold using HTML format
                      // $1 returns the regular's expression outer parenthesis value
                      int startIndex = m.start();
                      description = m.replaceAll("<b>$1</b>");
                      description = AndroidUtils.fillTextBox(paint, viewWidth, description, startIndex + 3);
                    }
                    mQueriedPoisStr.add(Html.fromHtml(name + "<br>" + description));
                  }

                  ArrayAdapter<Spanned> mAdapter = new ArrayAdapter<Spanned>(
                          // getBaseContext(), R.layout.queried_pois_item_1,
                          getBaseContext(), R.layout.queried_pois_item_1_searchactivity, mQueriedPoisStr);
                  lvResultPois.setAdapter(mAdapter);
                  txtResultsFound.setText("Results found [ " + mQueriedPoisStr.size() + " ]");
                }

                @Override
                public void onErrorOrCancel(String result) {
                  // no pois exist
                  finishSearch("No Points of Interest exist!", null);
                }

                @Override
                public void onUpdateStatus(String string, Cursor cursor) {
                  SimpleCursorAdapter adapter = new SimpleCursorAdapter(getBaseContext(), R.layout.queried_pois_item_1_searchactivity, cursor, new String[] { SearchManager.SUGGEST_COLUMN_TEXT_1 }, new int[] { android.R.id.text1 });
                  lvResultPois.setAdapter(adapter);
                  txtResultsFound.setText("Results found [ " + cursor.getCount() + " ]");
                }

              }, mSearchType, new GeoPoint(lat, lng), query, key);
      mSuggestionsTask.execute();

    }
  }

  /**
   * Returns an IAnyPlace object to the activity that initiated this search We use an IAnyPlace in order to allow the searching between GooglePlaces and AnyPlace POIs at the same time.
   *
   * @param result
   * @param place
   */
  private void finishSearch(String result, IPoisClass place) {
    if (place == null) {
      // we have an error
      Intent returnIntent = new Intent();
      returnIntent.putExtra("message", result);
      setResult(RESULT_CANCELED, returnIntent);
      finish();
    } else {
      Intent returnIntent = new Intent();
      returnIntent.putExtra("ianyplace", place);
      returnIntent.putExtra("message", result);
      setResult(RESULT_OK, returnIntent);
      finish();
    }
  }
}
