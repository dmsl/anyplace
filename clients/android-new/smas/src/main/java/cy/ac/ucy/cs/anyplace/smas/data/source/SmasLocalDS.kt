package cy.ac.ucy.cs.anyplace.smas.data.source

import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.smas.data.db.SmasDAO
import cy.ac.ucy.cs.anyplace.smas.data.db.entities.ChatMsgEntity
import cy.ac.ucy.cs.anyplace.smas.data.db.entities.DatabaseConverters.Companion.chatMsgtoEntity
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import cy.ac.ucy.cs.anyplace.smas.data.models.helpers.ChatMsgHelper
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SmasLocalDS @Inject constructor(private val DAO: SmasDAO) {

  fun readMsgs(): Flow<List<ChatMsgEntity>> {
    return DAO.readMsgs()
  }

  suspend fun insertMsg(msg: ChatMsg) {
    LOG.D4("DB: insert: ${msg.mid}: ${ChatMsgHelper.content(msg)}")
    DAO.insertChatMsg(chatMsgtoEntity(msg))
  }

  fun dropMsgs() {
    LOG.D2(TAG, "deleting all msgs")
    DAO.dropMsgs()
  }

  fun hasMsgs() : Boolean {
    val cnt = DAO.getMsgsCount()
    return cnt!=null && cnt>0
  }

  /**
   * Get last msg timestamp from local DB
   */
  fun getLastMsgTimestamp(): Long? {
    return DAO.lastMsgTimestamp()
  }

}
