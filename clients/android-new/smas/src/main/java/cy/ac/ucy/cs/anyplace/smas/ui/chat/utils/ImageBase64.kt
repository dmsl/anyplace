package cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.File

// TODO:PM:ATH merge w/ android-lib
class ImageBase64 {

    fun encodeToBase64(imageUri: Uri, context: Context): String {
        var encodedBase64 = ""

        if (imageUri != null) {
            val imageStream = context.contentResolver.openInputStream(imageUri!!)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            val baos = ByteArrayOutputStream()
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            val b = baos.toByteArray()
            encodedBase64 = Base64.encodeToString(b, Base64.DEFAULT)
        }

        return encodedBase64
    }

    fun decodeFromBase64(encodedBase64: String): Bitmap? {
        val bytes = Base64.decode(encodedBase64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun getMimeType(imageUri: Uri, context: Context): String {

      val extension: String? = if (imageUri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
      val mime = MimeTypeMap.getSingleton()
      mime.getExtensionFromMimeType(context.contentResolver.getType(imageUri!!))
    } else {
      MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(imageUri?.path)).toString())
    }

      return extension ?: ""
    }
}