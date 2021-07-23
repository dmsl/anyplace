package utils

import java.io.{File, FileInputStream, IOException}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class FileUtils @Inject()(cc: ControllerComponents,
                                conf: Configuration,
                                api: AnyplaceServerAPI)
  extends AbstractController(cc) {
  val RADIO_MAPS_FROZEN_DIR = conf.get[String]("radioMapFrozenDir")

  def getDirFrozenFloor(buid: String, floor_number: String): File = {
    return new File(RADIO_MAPS_FROZEN_DIR + api.URL_SEP + buid + api.URL_SEP + floor_number)
  }

  def getRadiomapFile(buid: String, floor_number: String): File = {
    return new File(RADIO_MAPS_FROZEN_DIR + api.URL_SEP + buid + api.URL_SEP +
      floor_number + api.URL_SEP + "indoor-radiomap.txt")
  }
  def getMeanFile(buid: String, floor_number: String): File = {
    return new File(RADIO_MAPS_FROZEN_DIR + api.URL_SEP + buid + api.URL_SEP +
      floor_number + api.URL_SEP + "indoor-radiomap-mean.txt")
  }

  def getRadioMapFileName(buid: String, floor_number: String): File = {
    return new File(getDirFrozenFloor(buid, floor_number).toString + api.URL_SEP + "indoor-radiomap.txt")
  }

  def getFilePos(fileName: String): Int = {
    val tokens = RADIO_MAPS_FROZEN_DIR.split("/")
    return fileName.indexOf(tokens(tokens.size - 1))
  }

  def getRadioMapRawFile() = ???

  def LoadFile(file: File): Array[Byte] = {
    val is = new FileInputStream(file)
    val length = file.length
    if (length > java.lang.Integer.MAX_VALUE) {
    }
    val bytes = Array.ofDim[Byte](length.toInt)
    var offset = 0
    var numRead = 0
    do {
      numRead = is.read(bytes, offset, bytes.length - offset)
      offset += numRead
    } while ((offset < bytes.length && numRead >= 0))
    if (offset < bytes.length) throw new IOException("Could not read file: " + file.getName)
    is.close()
    bytes
  }
}
