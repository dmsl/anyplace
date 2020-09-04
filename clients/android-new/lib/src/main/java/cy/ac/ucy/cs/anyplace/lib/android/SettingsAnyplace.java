package cy.ac.ucy.cs.anyplace.lib.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import cy.ac.ucy.cs.anyplace.lib.R;
//TODO: Add port and the rest from preferences. Logger data will also be accessed from here. This class is currently unnecessary.



public class SettingsAnyplace {

  private Context ctx;
  private String resPrefs;
  private SharedPreferences sharedPref;


  public SettingsAnyplace(Context ctx, String resPrefs){
    this.ctx = ctx;
    this.resPrefs = resPrefs;
    sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);

    //sharedPref = ctx.getSharedPreferences(resPrefs, Context.MODE_PRIVATE);
    //sharedPref = ctx.getSharedPreferences(resPrefs, Context.MODE_PRIVATE);

  }


  //TODO: Set the defaults in the string xml file
  public String getHost() {

    return sharedPref.getString("ap_url", ctx.getString(R.string.ap_default_server_url));
  }
}
