/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
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
* this software and associated documentation files (the “Software”), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package com.dmsl.anyplace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dmsl.anyplace.cache.AnyplaceCache;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper;
import com.dmsl.anyplace.nav.IAnyPlace;
import com.dmsl.anyplace.nav.PoisModel;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper.SearchTypes;
import com.dmsl.anyplace.provider.AnyplacePOIProvider;

public class SearchPOIActivity extends FragmentActivity {

	private TextView txtResultsFound;
	private ListView lvResultPois;
	private ListAdapter mAdapter;
	
	private List<String> mQueriedPoisStr;
	private List<PoisModel> mQueriedPois;
	
	private AnyplaceCache mAnyplaceCache = null;
	
	private FragmentActivity mThisActivity;
	
	private AnyPlaceSeachingHelper.SearchTypes mSearchType;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mThisActivity = this;
		
		setTitle("Points of Interest");
		setContentView(R.layout.activity_search_poi);
		
		txtResultsFound = (TextView)findViewById(R.id.txtResultsFound);
		
		lvResultPois = (ListView)findViewById(R.id.lvResultPois);
		lvResultPois.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				try{
					// TODO - handle the case of Google Place
					IAnyPlace place = null;
					if( mSearchType == SearchTypes.INDOOR_MODE ){
					    place = mQueriedPois.get(position);
					}else if( mSearchType == SearchTypes.OUTDOOR_MODE ){
						place = mAnyplaceCache.getGooglePlaces().results.get(position);
					}
					finishSearch("Success!", place);
				}catch( ArrayIndexOutOfBoundsException e ){
					Toast.makeText(getApplicationContext(), "Something went wrong with your selection!", Toast.LENGTH_LONG).show();
					mThisActivity.finish();
				}
			}
		});
		
		mAnyplaceCache = AnyplaceCache.getInstance(this);
		
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    
    private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// get the search type
			mSearchType = (SearchTypes) intent.getSerializableExtra("searchType");
			if( mSearchType == null ){
				finishSearch("No search type provided!", null);
			}
			// get the query string
			String query = intent.getStringExtra(SearchManager.QUERY); 
			// use the query to search the available places

			if ( mSearchType == SearchTypes.INDOOR_MODE ){
				// local poi search
				Collection<PoisModel> pois = mAnyplaceCache.getPois();
				if (pois != null && !pois.isEmpty()) {
					// we have pois to query for a match
					mQueriedPoisStr = new ArrayList<String>();
					mQueriedPois = new ArrayList<PoisModel>();
	
					for (PoisModel p : pois) {
						// check if the query matches the POI
						if (PoisModel.matchQueryPoi(query, p.name)) {
							// add the matched POI into the suggestions list
							mQueriedPois.add(p);	
							mQueriedPoisStr.add(p.name);
						}
					}
	
					mAdapter = new ArrayAdapter<String>(
							//getBaseContext(), R.layout.queried_pois_item_1,
					getBaseContext(), R.layout.queried_pois_item_1_searchactivity,
							mQueriedPoisStr);
					lvResultPois.setAdapter(mAdapter);
					txtResultsFound.setText("Results found [ " + mQueriedPoisStr.size() + " ]");
				}else {
					// no pois exist
					finishSearch("No Points of Interest exist!", null);
					return;
				}	
			}else if ( mSearchType == SearchTypes.OUTDOOR_MODE ){
				// google places search
				// TODO
				Toast.makeText(getBaseContext(), "No Google Places search at the moment!", Toast.LENGTH_SHORT).show();
				finishSearch("Google Places searching not supported yet!", null);
			} 

		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// AT THE MOMENT WE USE THE STATIC METHOD searchBySelectedSuggestion()
			
			// Handle a suggestions click (because the suggestions all use ACTION_VIEW)
			// get the information of the place selected which is passed as INTENT_DATA
			String data = intent.getDataString(); 
			// get the IAnyPlace object
			IAnyPlace place = AnyplacePOIProvider.searchBySelectedSuggestion(data);
			if( place != null ){
				finishSearch("Success!",place);
			}else{
				finishSearch("Point of Interested selected cannot be found!",null);
			}
		}
	}
    
    /**
     * Returns an IAnyPlace object to the activity that initiated this search
     * We use an IAnyPlace in order to allow the searching between GooglePlaces
     * and AnyPlace POIs at the same time.
     * 
     * @param result
     * @param poi
     */
    private void finishSearch(String result, IAnyPlace place){
    	if( place==null ){
    		// we have an error
    		Intent returnIntent = new Intent();
			returnIntent.putExtra("message", result);
			setResult(RESULT_CANCELED, returnIntent);
			mThisActivity.finish();
		}else{
			Intent returnIntent = new Intent();
			returnIntent.putExtra("ianyplace", place);
			returnIntent.putExtra("message", result);
			setResult(RESULT_OK, returnIntent);
			mThisActivity.finish();
		}
    }
    
    
    
  
 
}
