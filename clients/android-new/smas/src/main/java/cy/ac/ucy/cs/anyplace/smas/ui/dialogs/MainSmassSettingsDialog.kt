package cy.ac.ucy.cs.anyplace.smas.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.smas.databinding.DialogSmassMainSettingsBinding
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsNavigationActivity
import java.lang.IllegalStateException

internal enum class SettingsUi {
  Main,
  Chat,
}

class MainSmassSettingsDialog : DialogFragment() {

  companion object {
    const val KEY_FROM = "key.from"
    const val FROM_MAIN = "smass.main"
    const val FROM_CHAT = "smass.chat"

    fun SHOW(fragmentManager: FragmentManager, from: String) {
      val args = Bundle()
      args.putString(KEY_FROM, from)
      val dialog = MainSmassSettingsDialog()
      dialog.arguments = args
      dialog.show(fragmentManager, from)
    }
  }

  var _binding : DialogSmassMainSettingsBinding ?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogSmassMainSettingsBinding.inflate(LayoutInflater.from(context))

      handleArguments()

      val builder= AlertDialog.Builder(it)
      isCancelable = true
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      binding.buttonLogout.isEnabled = false
      setup()

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  private var settingsUi = SettingsUi.Main
  private fun handleArguments() {
    val bundle = requireArguments()
    // if (bundle.containsKey(KEY_FROM)) {
    //   val fromActivity = bundle.getString(KEY_FROM)
    //   when (fromActivity) {
    //     FROM_MAIN -> {  settingsUi = SettingsUi.Main  }
    //     FROM_CHAT -> {  settingsUi = SettingsUi.Chat }
    //   }
    // }
  }

  private fun setup() {
    setupLocalizationSettings()
  }

  private fun setupLocalizationSettings() {
    binding.buttonCvLocalization.setOnClickListener {
      startActivity(Intent(requireActivity(), SettingsNavigationActivity::class.java))
    }
  }

}
