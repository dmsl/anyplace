package cy.ac.ucy.cs.anyplace.smas.ui.chat.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import java.io.ByteArrayOutputStream
import java.io.File


// TODO:PM:ATH merge w/ android-lib
class ImageBase64 {

   fun resize(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    var image = image
    return if (maxHeight > 0 && maxWidth > 0) {
      val width = image.width
      val height = image.height
      val ratioBitmap = width.toFloat() / height.toFloat()
      val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
      var finalWidth = maxWidth
      var finalHeight = maxHeight
      if (ratioMax > 1) {
        finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
      } else {
        finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
      }
      image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
      image
    } else {
      image
    }
  }

  private fun rotateBm(source: Bitmap, angle: Int) : Bitmap{
    val matrix = Matrix()
    matrix.postRotate(angle.toFloat())
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
  }

  private fun returnRotatedBm(imageUri: Uri?, imageBm: Bitmap, context: Context) : Bitmap {
    if (imageUri != null) {
      val inputStream = context.contentResolver.openInputStream(imageUri)
      if (inputStream != null) {
        val exif = ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        when (orientation) {
          ExifInterface.ORIENTATION_ROTATE_270 -> return rotateBm(imageBm, 270)
          ExifInterface.ORIENTATION_ROTATE_180 -> return rotateBm(imageBm, 180)
          ExifInterface.ORIENTATION_ROTATE_90 -> return rotateBm(imageBm, 90)
        }
      }
    }
    return imageBm
  }

  fun encodeToBase64(imageUri: Uri?, context: Context): String {
    var encodedBase64 = ""

    if (imageUri != null) {
      val imageStream = context.contentResolver.openInputStream(imageUri!!)
      val selectedImage = BitmapFactory.decodeStream(imageStream)
      val rotatedBm = returnRotatedBm(imageUri, selectedImage, context)
      // val compressedBm = resize(rotatedBm, 1000, 1000)
      val baos = ByteArrayOutputStream()
      rotatedBm.compress(Bitmap.CompressFormat.JPEG, 50, baos)
      val b = baos.toByteArray()
      encodedBase64 = Base64.encodeToString(b, Base64.DEFAULT)
    }

    return encodedBase64
  }

  fun decodeFromBase64(encodedBase64: String): Bitmap? {
    var bytes = ByteArray(0)
    try {
      bytes = Base64.decode(encodedBase64, Base64.DEFAULT)
    } catch (e: IllegalArgumentException) {
      LOG.E("Image cannot be displayed.")
    }
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