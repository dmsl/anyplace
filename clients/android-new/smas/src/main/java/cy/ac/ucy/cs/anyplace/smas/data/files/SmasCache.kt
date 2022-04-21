package cy.ac.ucy.cs.anyplace.smas.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cy.ac.ucy.cs.anyplace.lib.android.cache.Cache
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.resize
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import java.io.File
import java.io.FileOutputStream

/**
 * File Cache for SMAS (extending anyplace-lib [Cache])
 */
class SmasCache(ctx: Context): Cache(ctx) {
  private val chatDir get() = "$baseDir/chat"

  //Images from the chat
  private fun dirChatImg(): String {
    return "$chatDir/img/"
  }

  private fun imgPath(mid: String, ext: String): String {
    return "${dirChatImg()}${mid}.$ext"
  }

  private fun imgPathTiny(mid: String, ext: String): String {
    return "${dirChatImg()}${mid}tiny.$ext"
  }

  private fun dirChatImgExists(): Boolean {
    return File(dirChatImg()).exists()
  }

  private fun imgExists(mid: String, ext: String): Boolean {
    return File(imgPath(mid, ext)).exists()
  }

  private fun imgTinyExists(mid : String, ext: String) : Boolean{
    return File(imgPathTiny(mid,ext)).exists()
  }

  fun saveImg(message: ChatMsg): Boolean {
    if (!dirChatImgExists())
      File(dirChatImg()).mkdirs()

    val bitmap : Bitmap?
    if (!imgExists(message.mid, message.mexten)) {
      val imgPath = imgPath(message.mid, message.mexten)
      val file = File(imgPath)
      val imgPathTiny = imgPathTiny(message.mid, message.mexten)
      val fileTiny = File(imgPathTiny)
      try {
        val out = FileOutputStream(file)
        bitmap = message.msg?.let { utlImg.decodeBase64(it) }
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()

        val outTiny = FileOutputStream(fileTiny)
        val compressedImg = bitmap?.resize(400,400)
        compressedImg?.compress(Bitmap.CompressFormat.JPEG, 50, outTiny)
        outTiny.flush()
        outTiny.close()
      } catch (e: Exception) {
        LOG.E(TAG, "saveImage: $file: ${e.message}")
        return false
      }
    }

    return true
  }

  fun readBitmap(message: ChatMsg): Bitmap? {
    var bitmap: Bitmap? = null
    if (imgExists(message.mid, message.mexten)) {
      val file = File(imgPath(message.mid, message.mexten))
      val bmOptions = BitmapFactory.Options()
      bitmap = BitmapFactory.decodeFile(file.absolutePath, bmOptions)
    }else{
      LOG.D(TAG,"Image with id ${message.mid} was not found")
    }
    return bitmap
  }

  fun readBitmapTiny(message: ChatMsg) : Bitmap?  {
     var bitmap: Bitmap? = null
     if (imgTinyExists(message.mid, message.mexten)) {
       val fileTiny = File(imgPathTiny(message.mid, message.mexten))
       val bmOptions = BitmapFactory.Options()
       bitmap = BitmapFactory.decodeFile(fileTiny.absolutePath, bmOptions)
     }else{
       LOG.D(TAG,"Tiny img with id ${message.mid} was not found")
     }
    return bitmap
  }
}