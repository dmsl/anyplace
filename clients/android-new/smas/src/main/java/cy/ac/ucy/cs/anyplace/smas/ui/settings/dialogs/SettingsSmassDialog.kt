package cy.ac.ucy.cs.anyplace.smas.ui.settings.dialogs
// userDS.readUser.first()

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsNavigationActivity
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.BuildConfig
import cy.ac.ucy.cs.anyplace.smas.databinding.DialogSettingsSmasBinding
import cy.ac.ucy.cs.anyplace.smas.extensions.appSmas
import cy.ac.ucy.cs.anyplace.smas.ui.settings.SettingsChatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal enum class SettingsUi {
  Main,
  Chat,
}

class MainSmasSettingsDialog : DialogFragment() {

  companion object {
    const val KEY_FROM = "key.from"
    const val FROM_MAIN = "smas.main"
    const val FROM_CHAT = "smas.chat"

    fun SHOW(fragmentManager: FragmentManager, from: String) {
      val args = Bundle()
      args.putString(KEY_FROM, from)
      val dialog = MainSmasSettingsDialog()
      dialog.arguments = args
      dialog.show(fragmentManager, from)
    }
  }

  var _binding : DialogSettingsSmasBinding ?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogSettingsSmasBinding.inflate(LayoutInflater.from(context))

      handleArguments()

      val builder= AlertDialog.Builder(it)
      isCancelable = true
      builder.setView(binding.root)
      val dialog = builder.create()
      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      binding.btnLogout.isEnabled = false
      setup()

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }

  private var settingsUi = SettingsUi.Main
  // TODO: when back from specific settings: close dialog automatically
  // must pass a message for that (in a bundle probably)
  private fun handleArguments() {
    // val bundle = requireArguments()
    // if (bundle.containsKey(KEY_FROM)) {
    //   val fromActivity = bundle.getString(KEY_FROM)
    //   when (fromActivity) {
    //     FROM_MAIN -> {  settingsUi = SettingsUi.Main  }
    //     FROM_CHAT -> {  settingsUi = SettingsUi.Chat }
    //   }
    // }
  }

  private fun setup() {
    setupMapSettings()
    setupChatUser()
    setupChatSettings()

    // misc:
    setupLashfireLink()
    // setupAnyplaceLink()
    setupVersion()
  }

  private fun setupChatUser() {
    CoroutineScope(Dispatchers.Main).launch {
      val chatUser = requireActivity().appSmas.chatUserDS.readUser.first()
      if (chatUser.sessionkey.isNotBlank()) {
        binding.user = chatUser
        binding.tvAccountType.isVisible = true
        binding.tvTitleAccountType.isVisible = true
        setupChatUserLogout()
      }
    }
  }

  private fun setupMapSettings() {
    binding.btnMapSettings.setOnClickListener {
      startActivity(Intent(requireActivity(), SettingsNavigationActivity::class.java))
    }
  }

  // private fun setupServerSettings() TODO

  private fun setupChatSettings() {
    binding.btnSettingsChat.setOnClickListener {
      startActivity(Intent(requireActivity(), SettingsChatActivity::class.java))
    }
  }

  // private fun setupAnyplaceLink() {
  //   binding.btnAboutAnyplace.setOnClickListener {
  //     startActivity(Intent(Intent.ACTION_VIEW,
  //             Uri.parse(getString(R.string.url_anyplace_about))))
  //   }
  // }

  private fun setupLashfireLink() {
    binding.btnAboutLashfire.setOnClickListener {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://lashfire.eu/")))
    }
  }

  private fun setupVersion() {
    val prettyVersion = getString(R.string.smas_version, BuildConfig.VERSION_NAME)
    binding.btnVersionSmas.text = prettyVersion
  }

  private fun setupChatUserLogout() {
    binding.btnLogout.isEnabled = true
    binding.btnLogout.setOnClickListener {
      CoroutineScope(Dispatchers.Main).launch {
        val msg: String
        val chatUserDS = requireActivity().appSmas.chatUserDS
        val user = chatUserDS.readUser.first()
        if (user.sessionkey.isNotBlank()) {
          msg = "Logging out ${app.dsUser.readUser.first().name}.."
          chatUserDS.deleteUser()
          dialog?.dismiss()
        } else {
          msg = "No logged in user."
        }
        Toast.makeText(requireActivity(), msg, Toast.LENGTH_SHORT).show()
      }
    }
  }

}
