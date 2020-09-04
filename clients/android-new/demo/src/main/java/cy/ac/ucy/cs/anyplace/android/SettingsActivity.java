package cy.ac.ucy.cs.anyplace.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
  private final String TAG = SettingsActivity.class.getSimpleName();
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);
    getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings, new SettingsFragment())
            .commit();
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    // View refreshBuilding = (View) findViewById(R.id.r_building);
    //
    // Log.e(TAG, refreshBuilding.toString());

    // refreshBuilding.setOnClickListener(new View.OnClickListener() {
    //   @Override
    //   public void onClick(View v) {
    //
    //     //TODO: Refresh building
    //
    //
    //   }
    // });

    // View refreshMap = (View) findViewById(R.id.r_map);
    //
    // refreshMap.setOnClickListener(new View.OnClickListener() {
    //   @Override
    //   public void onClick(View v) {
    //
    //     //TODO: Refresh Map pois
    //
    //
    //   }
    // });
    //
    // View deleteRadiomaps = (View) findViewById(R.id.d_radiomaps);
    //
    // deleteRadiomaps.setOnClickListener(new View.OnClickListener() {
    //   @Override
    //   public void onClick(View v) {
    //
    //     //TODO: Delete Radiomaps
    //
    //
    //   }
    // });
    //
    // View deleteFloorplans = (View) findViewById(R.id.d_floorplans);
    //
    // deleteFloorplans.setOnClickListener(new View.OnClickListener() {
    //   @Override
    //   public void onClick(View v) {
    //
    //     //TODO: Delete Floor plans
    //
    //
    //   }
    // });

  }


  public static class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = SettingsFragment.class.getSimpleName() ;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(TAG, rootKey +"");
        //setPreferencesFromResource(R.xml.root_preferences, rootKey);
        setPreferencesFromResource(R.xml.preferences_logger, rootKey);
      //TODO: Add the listeners for the options in preferences

      //   Preference pref = (Preference) findPreference("refresh_building");
      // assert pref != null;
      // pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      // {
      //   public boolean onPreferenceClick(Preference pref)
      //   {
      //
      //      if (isAdded()) {
      //         //TODO:
      //       } else {
      //         Log.e(TAG, "No activity in the settings fragment");
      //       }
      //      return true;
      //   }
      // });
      //
      // Preference pref2 = (Preference) findPreference("refresh_map");
      // assert pref2 != null;
      // pref2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      // {
      //   public boolean onPreferenceClick(Preference pref2)
      //   {
      //
      //     if (isAdded()) {
      //       //TODO:
      //     } else {
      //       Log.e(TAG, "No activity in the settings fragment");
      //     }
      //     return true;
      //   }
      // });
      //
      // Preference pref3 = (Preference) findPreference("delete_radiomaps");
      // assert pref3 != null;
      // pref3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      // {
      //   public boolean onPreferenceClick(Preference pref3)
      //   {
      //
      //     if (isAdded()) {
      //       //TODO:
      //     } else {
      //       Log.e(TAG, "No activity in the settings fragment");
      //     }
      //     return true;
      //   }
      // });
      //
      // Preference pref4 = (Preference) findPreference("delete_floorplans");
      // assert pref4 != null;
      // pref4.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      // {
      //   public boolean onPreferenceClick(Preference pref4)
      //   {
      //
      //     if (isAdded()) {
      //       //TODO:
      //     } else {
      //       Log.e(TAG, "No activity in the settings fragment");
      //     }
      //     return true;
      //   }
      // });



      }




  }
}