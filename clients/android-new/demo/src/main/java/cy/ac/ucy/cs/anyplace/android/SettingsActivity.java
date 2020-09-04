package cy.ac.ucy.cs.anyplace.android;

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
    // refreshBuilding.setOnClickListener(new View.OnClickListener() {
    //   @Override
    //   public void onClick(View v) {
    //
    //     //TODO: Refresh building
    //
    //
    //   }
    // });
    //
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
       setPreferencesFromResource(R.xml.root_preferences, rootKey);

    }
  }
}