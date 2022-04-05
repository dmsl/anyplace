package cy.ac.ucy.cs.anyplace.smas.extensions

import android.app.Activity
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore

val Activity.appSmas: SmasApp get() = this.application as SmasApp
val Activity.chatPrefsDS: ChatPrefsDataStore get() = this.appSmas.dsChat