package cy.ac.ucy.cs.anyplace.smas.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.smas.databinding.DialogChatBinding

class AlertDialog(): DialogFragment() {

  companion object {

    fun SHOW(fragmentManager: FragmentManager) {
      val dialog = AlertDialog()
      dialog.show(fragmentManager, "")
    }
  }

  var _binding : DialogChatBinding ?= null
  private val binding get() = _binding!!

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogChatBinding.inflate(LayoutInflater.from(context))

      // NOT WORKING
      isCancelable = true


      // Use the Builder class for convenient dialog construction
      val builder = AlertDialog.Builder(it)
      builder.setMessage("Send alert to nearby first responders?")
              .setPositiveButton("Yes",
                      { dialog, id ->
                        // TODO: send alert
                      })
              .setNegativeButton("Cancel") { dialog, id -> }
      return builder.create()
    }?: throw IllegalStateException("$TAG Activity is null.")
  }
}
