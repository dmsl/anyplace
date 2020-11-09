package cy.ac.ucy.cs.anyplace.navigator;

import android.app.Application;
import android.content.SharedPreferences;

import cy.ac.ucy.cs.anyplace.lib.Anyplace;
import cy.ac.ucy.cs.anyplace.lib.android.LOG;

public class AnyplaceApp extends Application {
  private Anyplace client = null;

  public void initializeAnyplace(){
    //TODO: initialize with shared preferences

    SharedPreferences pref = getSharedPreferences("LoggerPreferences", MODE_PRIVATE);
     this.client = new Anyplace(pref.getString("server_ip_address", "ap.cs.ucy.ac.cy"), "443",getApplicationContext().getCacheDir().getAbsolutePath());

  }

  public Anyplace getAnyplace() {
    if (client == null){
      LOG.e("Anyplace not initialized");

    }
    return client;
  }
}
