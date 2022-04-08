package cy.ac.ucy.cs.anyplace.smas.data.bindingadapters

import android.widget.TextView
import androidx.databinding.BindingAdapter
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.setTextOrHide
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUser

class ChatUserBinding {
  companion object {
    @BindingAdapter("readUserid", requireAll = true)
    @JvmStatic
    fun readUserd(
      view:TextView,
      user: ChatUser?) {
      // BUG: it's null
      LOG.D(TAG, "UserBinding: $user")
      view.setTextOrHide(user?.uid, "")
    }
  }
}