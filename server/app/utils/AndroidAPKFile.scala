/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */
package utils

import java.io.File

import java.util.Comparator

import java.util.Date

import java.util.Locale

// CLR:PM
object AndroidAPKFile {

  class AndroidAPKComparator extends Comparator[AndroidAPKFile] {

    override def compare(thiss: AndroidAPKFile, that: AndroidAPKFile): Int =
      try {
        val vThis: String = thiss.getVersion().substring(1)
        val vThat: String = that.getVersion().substring(1)
        val segsThis: Array[String] = vThis.split("[.]")
        val segsThat: Array[String] = vThat.split("[.]")
        for (i <- 0 until segsThis.length) {
          val a: Int = java.lang.Integer.parseInt(segsThis(i))
          val b: Int = java.lang.Integer.parseInt(segsThat(i))
          if (a < b) {
            -1
          } else if (a > b) {
            1
          }
        }
        if (thiss.isRelease()) -1 else 1
      } catch {
        case e: NumberFormatException => -1

      }

  }

}

class AndroidAPKFile(private var mFile: File) {
  private var mFileBasename: String = mFile.getAbsolutePath.substring(
    mFile.getAbsolutePath.lastIndexOf(File.separatorChar) + 1)
  val segs: Array[String] = mFileBasename.split("_")
  private var mUrl: String = _
  private var mVersion: String = segs(2)
  private var mIsRelease: Boolean = segs(3).toLowerCase(Locale.ENGLISH).contains("release")
  private var mIsDev: Boolean = !mIsRelease
  private var mDate: Date = new Date(mFile.getAbsoluteFile.lastModified())

  def getVersion(): String = mVersion
  def isRelease(): Boolean = mIsRelease
  def isDev(): Boolean = mIsDev
  def getFile(): File = mFile
  def getDate(): Date = mDate
  def getFilePath(): String = mFile.getAbsolutePath
  def getFilePathBasename(): String = mFileBasename
  def setDownloadUrl(url: String): Unit = mUrl = url
  def getDownloadUrl(): String = mUrl
}

// Android apks are in the form:
// anyplace_android_vX.Y.Z_{DEV,RELEASE}.apk