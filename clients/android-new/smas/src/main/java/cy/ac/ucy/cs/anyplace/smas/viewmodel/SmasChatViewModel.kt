package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.*
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.UserLocations
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.SmasMessages
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
        private val retrofitHolderChat: RetrofitHolderChat,
        private val miscDS: MiscDataStore,
  ): AndroidViewModel(app) {

  private val messages by lazy { SmasMessages(app as SmasApp, retrofitHolderChat, repoChat) }

  fun pullChatMsgsONCE()  {
    LOG.E()
    viewModelScope.launch {
      messages.getSC()
    }
  }

  fun collectMessages()  {
    viewModelScope.launch {
      messages.collect(app, this@SmasChatViewModel)
    }
  }

}