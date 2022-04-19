package cy.ac.ucy.cs.anyplace.smas.consts

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.smas.R

open class CHAT(ctx: Context) : CONST(ctx) {

  //// ROOM
  companion object {
    const val DB_SMAS_MSGS= "messages"
  }

  val DB_SMAS_NAME = "smas_db"

  // PREFERENCES
  //// CHAT SERVER
  val PREF_CHAT_SERVER = ctx.getString(R.string.pref_chat_server)
  val PREF_CHAT_SERVER_PROTOCOL = ctx.getString(R.string.pref_chat_server_protocol)
  val PREF_CHAT_SERVER_HOST = ctx.getString(R.string.pref_chat_server_host)
  val PREF_CHAT_SERVER_PORT = ctx.getString(R.string.pref_chat_server_port)
  val PREF_CHAT_SERVER_VERSION = ctx.getString(R.string.pref_chat_server_version)
  // CHAT SETTINGS
  /** from [ChatMsgs.mdelivery] */
  val PREF_CHAT_MDELIVERY= ctx.getString(R.string.pref_chat_mdelivery)
  val PREF_CHAT_USER= ctx.getString(R.string.pref_chat_user)

  ////// CHAT SERVER: DEFAULTS
  val DEFAULT_PREF_CHAT_SERVER_PROTOCOL = ctx.getString(R.string.default_pref_chat_server_protocol)
  val DEFAULT_PREF_CHAT_SERVER_HOST = ctx.getString(R.string.default_pref_chat_server_host)
  val DEFAULT_PREF_CHAT_SERVER_PORT = ctx.getString(R.string.default_pref_chat_server_port)

  val DEFAULT_PREF_CHAT_MDELIVERY = ctx.getString(R.string.default_pref_chat_mdelivery)
}