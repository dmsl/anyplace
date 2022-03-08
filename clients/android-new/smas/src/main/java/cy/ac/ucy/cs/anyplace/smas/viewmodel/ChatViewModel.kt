package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.*
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.android.data.store.*
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatVersion
import cy.ac.ucy.cs.anyplace.smas.data.models.UserLocations
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.SmasVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
class ChatViewModel @Inject constructor(
        app: Application,
        private val repoChat: RepoChat,
        private val retrofitHolderChat: RetrofitHolderChat,
        private val miscDS: MiscDataStore,
  ): AndroidViewModel(app) {

}