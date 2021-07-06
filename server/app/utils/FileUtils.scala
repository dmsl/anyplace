package utils

import java.io.File

import play.Play

object FileUtils {
  val RADIO_MAPS_FROZEN_DIR = Play.application().configuration().getString("radioMapFrozenDir")

  def getDirFrozenFloor(buid: String, floor_number: String): File = {
    return new File(RADIO_MAPS_FROZEN_DIR + AnyplaceServerAPI.URL_SEP + buid + AnyplaceServerAPI.URL_SEP + floor_number)
  }

  def getRadiomapFile(buid: String, floor_number: String): File = {
    return new File(RADIO_MAPS_FROZEN_DIR + AnyplaceServerAPI.URL_SEP + buid + AnyplaceServerAPI.URL_SEP +
      floor_number + AnyplaceServerAPI.URL_SEP + "indoor-radiomap.txt")
  }
  def getMeanFile(buid: String, floor_number: String): File = {
    return new File(RADIO_MAPS_FROZEN_DIR + AnyplaceServerAPI.URL_SEP + buid + AnyplaceServerAPI.URL_SEP +
      floor_number + AnyplaceServerAPI.URL_SEP + "indoor-radiomap-mean.txt")
  }

  def getRadioMapFileName(buid: String, floor_number: String): File = {
    return new File(getDirFrozenFloor(buid, floor_number).toString + AnyplaceServerAPI.URL_SEP + "indoor-radiomap.txt")
  }

  def getFilePos(fileName: String): Int = {
    val tokens = RADIO_MAPS_FROZEN_DIR.split("/")
    return fileName.indexOf(tokens(tokens.size - 1))
  }

  def getRadioMapRawFile() = ???
}
