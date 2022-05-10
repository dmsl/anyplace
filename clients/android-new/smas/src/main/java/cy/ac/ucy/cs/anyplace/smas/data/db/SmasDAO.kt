package cy.ac.ucy.cs.anyplace.smas.data.db
import androidx.room.*
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.db.entities.ChatMsgEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmasDAO {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChatMsg(tuple: ChatMsgEntity)

  @Query("SELECT * FROM ${CHAT.DB_SMAS_MSGS} ORDER BY time ASC")
  fun readMsgs(): Flow<List<ChatMsgEntity>>

  @Query("DELETE FROM ${CHAT.DB_SMAS_MSGS}")
  fun dropMsgs()

  @Query("SELECT time FROM ${CHAT.DB_SMAS_MSGS} ORDER BY time DESC LIMIT 1")
  fun lastMsgTimestamp(): Long?

  @Query("SELECT COUNT(mid) FROM ${CHAT.DB_SMAS_MSGS}")
  fun getMsgsCount(): Int?

}
