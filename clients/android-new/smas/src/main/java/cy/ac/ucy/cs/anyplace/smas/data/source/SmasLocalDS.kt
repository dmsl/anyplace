package cy.ac.ucy.cs.anyplace.smas.data.source

import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.smas.data.db.SmasDAO
import cy.ac.ucy.cs.anyplace.smas.data.db.entities.ChatMsgEntity
import cy.ac.ucy.cs.anyplace.smas.data.db.entities.DatabaseConverters.Companion.chatMsgtoEntity
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SmasLocalDS @Inject constructor(private val DAO: SmasDAO) {

  fun readMsgs(): Flow<List<ChatMsgEntity>> {
    return DAO.readMsgs()
  }

  suspend fun insertMsg(msg: ChatMsg) {
    LOG.D2("insert: ${msg.mid}")
    DAO.insertChatMsg(chatMsgtoEntity(msg))
  }

  suspend fun dropMsgs() {
    LOG.D2("deleting all msgs")
    DAO.dropMsgs()
  }
}
