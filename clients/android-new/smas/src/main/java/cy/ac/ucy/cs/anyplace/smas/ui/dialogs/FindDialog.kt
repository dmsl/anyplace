package cy.ac.ucy.cs.anyplace.smas.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cy.ac.ucy.cs.anyplace.lib.android.data.Repository
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.smas.databinding.DialogChatBinding


class FindDialog(
        private val repo: Repository,
        // TODO: create a new DataStore here and pass it
        // private val dataStoreCv: DataStoreCv
        ): DialogFragment() {

  companion object {
    // TODO what keys we need?
    private const val KEY_SPACE= "space"

    /**
     * TODO
     */
    fun SHOW(
            fragmentManager: FragmentManager,
            repo: Repository,
            // dataStoreCv: DataStoreCv,
            // SH: SpaceHelper?,
            // FSH: FloorsHelper?,
            // FH: FloorHelper?
    ) {
      val args = Bundle()

      //   args.putString(KEY_SPACE, sh.toString())
      // SH?.let { sh ->
      //   args.putString(KEY_SPACE, sh.toString())
      //   // FSH?.let { args.putString(KEY_FLOORS, it.toString()) }
      //   // FH?.let { args.putString(KEY_FLOOR, it.toString()) }
      // }

      val dialog = FindDialog(repo)
      dialog.arguments = args
      // val test = dialog.requireArguments().getString(KEY_FROM)
      dialog.show(fragmentManager, "")
    }
  }

  // var spaceH: SpaceHelper?= null
  // var floorsH: FloorsHelper?= null
  // var floorH: FloorHelper?= null

  var _binding : DialogChatBinding ?= null
  private val binding get() = _binding!!
  // var fromCvLogger = true

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      _binding = DialogChatBinding.inflate(LayoutInflater.from(context))


      // NOT WORKING
      val builder= AlertDialog.Builder(it)
      isCancelable = true
      builder.setView(binding.root)
      val dialog = builder.create()

      dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

      // context?.theme?.applyStyle(R.style.ChatDialogTheme, true)
      if (dialog != null) {
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = 800
        // val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog.window?.setLayout(width, height)
      }

      // setupRadioButton()
      // setupConfirmButton()

      return dialog
    }?: throw IllegalStateException("$TAG Activity is null.")
  }


  override fun onStart() {
    super.onStart()
  }

  private fun setupRadioButton() {
    val bundle = requireArguments()
    if (bundle.containsKey(KEY_SPACE)) {
      // spaceH = IntentExtras.getSpace(requireActivity(), repo, bundle, KEY_SPACE)
      // if (spaceH != null) {
      //   val SH = spaceH!!
      //   LOG.D(TAG_METHOD, "Space is ${SH.space.name}")
      //   floorsH=IntentExtras.getFloors(spaceH, bundle, KEY_FLOORS)
      //   binding.radioButtonSpace.text=getString(R.string.for_var_var, SH.prettyType, SH.space.name)
      //   binding.radioButtonSpace.visibility=View.VISIBLE
      //   binding.radioButtonFloor.isChecked = true
      //
      //   floorH = IntentExtras.getFloor(spaceH, bundle, KEY_FLOOR)
      //   if (floorH!=null) {
      //     LOG.D(TAG_METHOD, "Floor ${floorH?.prettyFloorNumber()}")
      //     binding.radioButtonFloor.text = getString(R.string.for_var_var,
      //             floorH?.prettyFloorNumber(), " of ${SH.space.name}")
      //     binding.radioButtonFloor.visibility=View.VISIBLE
      //     binding.radioButtonFloor.isChecked = true
      //   }
      // }
    }
  }

  // TODO: on CvLoggerActivity (or fragment) resume: redraw the heatmap
  // (or set a boolean if we must redraw)
  private fun setupConfirmButton() {
   // val btn = binding.buttonClearCaches
   //  btn.setOnClickListener {
   //    // when {
   //    //   binding.radioButtonAll.isChecked -> { Cache(requireActivity()).deleteCvMapsLocal() }
   //    //   binding.radioButtonSpace.isChecked -> { floorsH?.clearCacheCvMaps() }
   //    //   binding.radioButtonFloor.isChecked -> { floorH?.clearCacheCvMaps() }
   //    // }
   //    //
   //    // dataStoreCv.setReloadCvMaps(true)
   //
   //    dismiss()
   //  }
  }
}
