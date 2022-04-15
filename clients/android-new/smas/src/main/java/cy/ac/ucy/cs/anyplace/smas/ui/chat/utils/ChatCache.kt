package cy.ac.ucy.cs.anyplace.smas.ui.chat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import java.io.File
import java.io.FileOutputStream

class ChatCache(val ctx: Context) {

  val imageHelper: ImageBase64 = ImageBase64()

  val chatDir get() = "${ctx.filesDir}/chat"

  //Images from the chat
  fun dirChatImg(): String {
    return "$chatDir/img/"
  }

  fun imgPath(mid: String, mexten: String): String {
    return "${dirChatImg()}${mid}.$mexten"
  }

  fun imgPathTiny(mid: String, mexten: String): String {
    return "${dirChatImg()}${mid}tiny.$mexten"
  }

  fun dirChatImgExists(): Boolean {
    return File(dirChatImg()).exists()
  }

  fun imgExists(mid: String, mexten: String): Boolean {
    return File(imgPath(mid, mexten)).exists()
  }

  fun imgTinyExists(mid : String, mexten: String) : Boolean{
    return File(imgPathTiny(mid,mexten)).exists()
  }

  fun saveImg(message: ChatMsg): Boolean {
    if (!dirChatImgExists())
      File(dirChatImg()).mkdirs()

    var bitmap : Bitmap? = null
    if (!imgExists(message.mid, message.mexten)) {
      val imgPath = imgPath(message.mid, message.mexten)
      val file = File(imgPath)
      val imgPathTiny = imgPathTiny(message.mid, message.mexten)
      val fileTiny = File(imgPathTiny)
      try {
        val out = FileOutputStream(file)
        bitmap = message.msg?.let { imageHelper.decodeFromBase64(it) }
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()

        val outTiny = FileOutputStream(fileTiny)
        val compressedImg = bitmap?.let { imageHelper.resize(it,400,400) }
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

  fun getBitmap(message: ChatMsg): Bitmap? {
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

  fun getBitmapTiny(message: ChatMsg) : Bitmap?  {
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