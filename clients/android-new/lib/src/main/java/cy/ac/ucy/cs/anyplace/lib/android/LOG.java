package cy.ac.ucy.cs.anyplace.lib.android;

import android.util.Log;



public class LOG {

  private static final String TAG = "anyplace";
  private static int level = 2;


  public static void i(String message){

    Log.i(TAG, message);

  }
  public static void i(int lvl, String message){


    if (lvl == level){
      Log.i(TAG, message);
    }

  }

  public static void e(String message){

    Log.e(TAG, message);

  }
  public static void e(int lvl, String message){


    if (lvl == level){
      Log.e(TAG, message);
    }

  }

  public static void d(String message){

    Log.d(TAG, message);

  }
  public static void d(int lvl, String message){


    if (lvl == level){
      Log.d(TAG, message);
    }

  }

}
