package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolder
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Extends [CvMapViewModel]:
 * - TODO here merge chat / messages
 */
@HiltViewModel
class SmasViewModel @Inject constructor(
        application: Application,
        repository: Repository,
        retrofitHolder: RetrofitHolder):
        CvMapViewModel(application, repository, retrofitHolder) {

  private val C by lazy { CONST(app) }

  override fun prefWindowLocalizationMillis(): Int {
    // TODO:PM modify properly for Smass?
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

}