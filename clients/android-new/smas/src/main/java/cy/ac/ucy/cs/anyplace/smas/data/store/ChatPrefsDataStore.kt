package cy.ac.ucy.cs.anyplace.smas.data.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.NetUtils
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat Backend Preferences
 */
@Singleton // INFO cannot be ViewModelScoped, as it is used by NetworkModule
class ChatPrefsDataStore @Inject constructor(@ApplicationContext private val ctx: Context)
  : PreferenceDataStore() {

  private val C by lazy { CHAT(ctx) }
  private val Context.dsChat by preferencesDataStore(name = C.PREF_CHAT_SERVER)
  private val datastore = ctx.dsChat

  private val validKeys = setOf(
          // BACKEND SETTINGS
          C.PREF_CHAT_SERVER_PROTOCOL,
          C.PREF_CHAT_SERVER_HOST,
          C.PREF_CHAT_SERVER_PORT,
          // MESSAGING SETTINGS
          C.PREF_CHAT_MDELIVERY,
          // TEMPORARY FLAGS
          C.FLAG_CHAT_NEWMSGS
  )

  // CHECK: is this needed?
  // these are not actual preferences. just placeholders to display some information
  // like backend version when displaying the connection status.
  private val ignoreKeys = setOf(C.PREF_SERVER_VERSION, C.FLAG_CHAT_NEWMSGS)

  private class Keys(c: CHAT) {
    val protocol= stringPreferencesKey(c.PREF_CHAT_SERVER_PROTOCOL)
    val host = stringPreferencesKey(c.PREF_CHAT_SERVER_HOST)
    val port = stringPreferencesKey(c.PREF_CHAT_SERVER_PORT)
    val mdelivery = stringPreferencesKey(c.PREF_CHAT_MDELIVERY)
    /** backend version */
    val version = stringPreferencesKey(c.PREF_CHAT_SERVER_VERSION)

    val flagNewMsgs = booleanPreferencesKey(c.FLAG_CHAT_NEWMSGS)
  }

  private val KEY = Keys(C)

  private fun ignoreKey(key: String?) = ignoreKeys.contains(key)

  private fun validKey(key: String?): Boolean {
    if (ignoreKey(key)) return false
    val found = validKeys.contains(key)
    if(!found) LOG.W(TAG, "Unknown key: $key")
    return found
  }

  override fun putString(key: String?, value: String?) {
    if (!validKey(key)) return
    runBlocking {
      datastore.edit {
        when (key) {
          C.PREF_CHAT_SERVER_HOST -> it[KEY.host] = value?: C.DEFAULT_PREF_CHAT_SERVER_HOST
          C.PREF_CHAT_SERVER_PORT -> {
            val storeValue : String =
                    if (NetUtils.isValidPort(value)) value!! else C.DEFAULT_PREF_CHAT_SERVER_PORT
            it[KEY.port] =  storeValue
          }
          C.PREF_SERVER_PROTOCOL -> {
            if(NetUtils.isValidProtocol(value))
              it[KEY.protocol] = value?: C.DEFAULT_PREF_CHAT_SERVER_PROTOCOL
          }

          C.PREF_CHAT_MDELIVERY -> it[KEY.mdelivery] = value?: C.DEFAULT_PREF_CHAT_MDELIVERY
        }
      }
    }
  }

  override fun getString(key: String?, defValue: String?): String? {
    if (!validKey(key)) return null
    return runBlocking(Dispatchers.IO) {
      val prefs = read.first()
      return@runBlocking when (key) {
        C.PREF_CHAT_SERVER_HOST -> prefs.host
        C.PREF_CHAT_SERVER_PORT -> prefs.port
        C.PREF_CHAT_SERVER_PROTOCOL -> prefs.protocol
        C.PREF_CHAT_MDELIVERY -> prefs.mdelivery
        else -> null
      }
    }
  }

  override fun putBoolean(key: String?, value: Boolean) { }
  override fun getBoolean(key: String?, defValue: Boolean): Boolean { return false }

  val read: Flow<ChatPrefs> = ctx.dsChat.data
          .catch { exception ->
            if (exception is IOException) {
              emit(emptyPreferences())
            } else { throw exception }
          }
          .map { preferences ->
            val protocol = preferences[KEY.protocol] ?: C.DEFAULT_PREF_CHAT_SERVER_PROTOCOL
            val host = preferences[KEY.host] ?: C.DEFAULT_PREF_CHAT_SERVER_HOST
            val port = preferences[KEY.port] ?: C.DEFAULT_PREF_CHAT_SERVER_PORT
            val mdelivery = preferences[KEY.mdelivery] ?: C.DEFAULT_PREF_CHAT_MDELIVERY
            val version = preferences[KEY.version]
            ChatPrefs(protocol, host, port, mdelivery, version)
          }

  /** Stores the backend version.
   * Not this is not a [KEY] as it cannot be modified by the XML UI.
   * Therefore:
   * - no handling in [getString]/[putString]
   * - is in [ignoreKeys]
   */
  suspend fun storeVersion(version: String) {
    ctx.dsChat.edit {
      it[KEY.version] = version
    }
  }

  private suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
    ctx.dsChat.edit { prefs -> prefs[key] = value }
  }

  // private suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
  //   ctx.dsChat.edit { prefs -> prefs[key] = value }
  // }


  suspend fun saveNewMsgs(value: Boolean) =
          saveBoolean(KEY.flagNewMsgs, value)

  val readHasNewMessages: Flow<Boolean> = ctx.dsChat.data
          .catch {  exception ->
            if (exception is IOException) {
              emit(emptyPreferences())
            } else { throw exception }
          }.map { prefs -> prefs[KEY.flagNewMsgs] ?: false }

}


data class ChatPrefs(
        val protocol: String,
        val host: String,
        val port: String,
        /** Message Delivery (see [ChatMsgs.mdelivery]) */
        val mdelivery: String,
        val version: String?,
)
