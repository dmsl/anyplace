package cy.ac.ucy.cs.anyplace.lib.android;

import android.content.Context;
import android.content.SharedPreferences;
//TODO: Add port and the rest from preferences. Logger data will also be accessed from here



public class SettingsAnyplace {

  private Context ctx;
  private String resPrefs;
  private SharedPreferences sharedPref;
  private final String DEF_URL= "ap-dev.cs.ucy.ac.cy";

  public SettingsAnyplace(Context ctx, String resPrefs){
    this.ctx = ctx;
    this.resPrefs = resPrefs;

    //sharedPref = ctx.getSharedPreferences(resPrefs, Context.MODE_PRIVATE);
    sharedPref = ctx.getSharedPreferences(resPrefs, Context.MODE_PRIVATE);

  }

  public String getHost() {

    return sharedPref.getString("ap_url", DEF_URL);
  }
}
