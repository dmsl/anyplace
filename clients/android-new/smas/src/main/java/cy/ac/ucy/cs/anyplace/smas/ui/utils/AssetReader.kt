package cy.ac.ucy.cs.anyplace.smas.ui.utils

import android.content.Context
import android.util.Log
//import com.example.messages.modules.Accounts
import cy.ac.ucy.cs.anyplace.smas.ui.tmp_models.Messages
import com.google.gson.Gson
import java.io.IOException
import java.lang.Exception

class AssetReader(val ctx: Context) {
  private val gson = Gson()

  companion object{
    private const val TAG = "Asset Reader"
  }

  fun getMessages(): Messages?{
    val str = getMessagesStr()
    str.let {
      try {
        return Gson().fromJson(str, Messages::class.java)
      } catch (e: Exception) {
        Log.d(TAG,"Failed to parse: $str")
      }
    }
    return null
  }

  // fun getAccounts(): Accounts?{
  //   val str = getAccountsStr()
  //   str.let {
  //     try {
  //       return Gson().fromJson(str, Accounts::class.java)
  //     } catch (e: Exception) {
  //       Log.d(TAG,"Failed to parse: $str")
  //     }
  //   }
  //   return null
  // }

  private fun getMessagesStr(): String?{
    return getJsonDataFromAsset(ctx, "dummy_messages.json")
  }

  // private fun getAccountsStr(): String?{
  //   return getJsonDataFromAsset(ctx, "dummy_accounts.json")
  // }

  private fun getJsonDataFromAsset(context: Context, filename: String): String? {
    val jsonString: String
    try {
      jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
      ioException.printStackTrace()
      Log.d(TAG, "Error reading: $filename: ${ioException.message}")
      return null
    }
    return jsonString
  }
}