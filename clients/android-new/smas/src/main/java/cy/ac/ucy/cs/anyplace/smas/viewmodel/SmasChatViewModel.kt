package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.*
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.UserLocations
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.MsgsGetUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles everything related to the Chat.
 *
 * [UserLocations] are handled by [SmasMainViewModel]
 *
 * TODO:AH will put stuff here.
 */
@HiltViewModel
class SmasChatViewModel @Inject constructor(
        private val app: Application,
        private val repoChat: RepoChat,
        private val RFH: RetrofitHolderChat,
        private val miscDS: MiscDataStore,
  ): AndroidViewModel(app) {

  private val utlMsgsGet by lazy { MsgsGetUtil(app as SmasApp, this, RFH, repoChat) }
  // TODO:ATH utlMsgSend: MsgsSendUtil

  fun fetchMessages() {
    LOG.E()
    viewModelScope.launch {
      utlMsgsGet.safeCall()
    }
  }

  /**
   * React to flow that is populated by [utlMsgsGet] safeCall
   */
  fun collectMessages()  {
    viewModelScope.launch {
      utlMsgsGet.collect(app)
    }
  }

}