package cy.ac.ucy.cs.anyplace.smas.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore for the Logged-in SMAS user
 */
@Singleton
class ChatUserDataStore @Inject constructor(@ApplicationContext private val ctx: Context) {

  private val C by lazy { CHAT(ctx) }
  private val Context.chatUserDS by preferencesDataStore(name = C.PREF_CHAT_USER)

  private class Keys(c: CHAT) {
    val uid = stringPreferencesKey(c.PREF_USER_ID)
    val sessionkey = stringPreferencesKey(c.PREF_USER_ACCESS_TOKEN)
  }
  private val KEY = Keys(C)

  val readUser: Flow<ChatUser> =
    ctx.chatUserDS.data
        .catch { exception ->
         if (exception is IOException)  { emit(emptyPreferences()) } else { throw exception }
        }
        .map {
          val sessionkey = it[KEY.sessionkey] ?: ""
          val uid = it[KEY.uid] ?: ""
          ChatUser(uid, sessionkey)
        }

  /** Stores a logged in user to the datastore */
  suspend fun storeUser(user: ChatUser) {
    ctx.chatUserDS.edit {
      it[KEY.uid] = user.uid
      it[KEY.sessionkey] = user.sessionkey
    }
  }

  /** Deletes the logged in user */
  suspend fun deleteUser() {  ctx.chatUserDS.edit { it.clear() } }
}
