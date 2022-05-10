package cy.ac.ucy.cs.anyplace.smas.extensions

import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore

val Activity.appSmas: SmasApp get() = this.application as SmasApp
val PreferenceFragmentCompat.appSmas: SmasApp get() = this.requireActivity().appSmas
val Activity.chatPrefsDS: ChatPrefsDataStore get() = this.appSmas.dsChat
val AndroidViewModel.appSmas: SmasApp get() = getApplication<SmasApp>()
