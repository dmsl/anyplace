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
package radiomapserver

import java.io.{BufferedReader, File, FileReader}
import java.util.{ArrayList, HashMap}

import utils.LPLogger

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.CollectionHasAsScala
//remove if not needed
// import scala.collection.JavaConversions._
import scala.util.control.Breaks._

class RadioMapMean(private val isIndoor: Boolean, @BeanProperty val defaultNaNValue: Int) {

  private var RadiomapMean_File: File = null

  private var MacAdressList: ArrayList[String] = new ArrayList[String]()

  private var LocationRSS_HashMap: HashMap[String, ArrayList[String]] = null

  private var GroupLocationRSS_HashMap: HashMap[Integer, HashMap[String, ArrayList[String]]] = new HashMap[Integer, HashMap[String, ArrayList[String]]]()

  private var OrderList: ArrayList[String] = new ArrayList[String]()

  def getMacAdressList(): ArrayList[String] = MacAdressList

  def getLocationRSS_HashMap(group: Int): HashMap[String, ArrayList[String]] = GroupLocationRSS_HashMap.get(group)

  def getOrderList(): ArrayList[String] = OrderList

  def getRadiomapMean_File(): File = this.RadiomapMean_File

  def ConstructRadioMap(inFile: File): Boolean = {
    if (!inFile.exists() || !inFile.canRead()) {
      return false
    }
    this.RadiomapMean_File = inFile
    this.OrderList.clear()
    this.MacAdressList.clear()
    var RSS_Values: ArrayList[String] = null
    var reader: BufferedReader = null
    var line: String = null
    var temp: Array[String] = null
    var group = -1
    var key: String = null
    var lastKey: String = null
    try {
      reader = new BufferedReader(new FileReader(inFile))
      var c = 0
      while ( {
        line = reader.readLine; line != null
      }) {
        breakable {
          c += 1
          if (line.trim() == "") {
            //continue (break from breakable inside loop)
            break()
          }
          line = line.replace(", ", " ")
          temp = line.split(" ")
          if (temp(0).trim() == "#") {
            if (temp(1).trim() == "NaN") {
              //continue (break from breakable inside loop)
              break()
            }
            if (temp.length < 5) {
              return false
            } else if (this.isIndoor &&
              (!temp(1).trim().equalsIgnoreCase("X") || !temp(2).trim().equalsIgnoreCase("Y"))) {
              return false
            }
            for (i <- 4 until temp.length) {
              if (!temp(i).matches("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}")) {
                return false
              }
              this.MacAdressList.add(temp(i))
            }
            //continue (break from breakable inside loop)
            break()
          }
          key = temp(0) + " " + temp(1)
          group = java.lang.Integer.parseInt(temp(2))
          RSS_Values = new ArrayList[String]()
          for (i <- 3 until temp.length) {
            RSS_Values.add(temp(i))
          }
          if (this.MacAdressList.size != RSS_Values.size) {
            return false
          }
          if (key != lastKey) {
            this.OrderList.add(key)
            lastKey = key
          }
          this.LocationRSS_HashMap = this.GroupLocationRSS_HashMap.get(group)
          if (this.LocationRSS_HashMap == null) {
            this.LocationRSS_HashMap = new HashMap[String, ArrayList[String]]()
            this.LocationRSS_HashMap.put(key, RSS_Values)
            this.GroupLocationRSS_HashMap.put(group, LocationRSS_HashMap)
            //continue (break from breakable inside loop)
            break()
          }
          this.LocationRSS_HashMap.put(key, RSS_Values)
        }
      }
      reader.close()
    } catch {
      case e: Exception => {
        LPLogger.debug("Error while constructing RadioMap: ")
        e.printStackTrace()
        return false
      }
    }
    true
  }

  def getGroupLocationRSS_HashMap(): HashMap[Integer, HashMap[String, ArrayList[String]]] = {
    GroupLocationRSS_HashMap
  }

  override def toString(): String = {
    var str = "MAC Adresses: "
    var temp: ArrayList[String] = null
    for (i <- 0 until MacAdressList.size) {
      str += MacAdressList.get(i) + " "
    }
    str += "\nLocations\n"
    for (location <- LocationRSS_HashMap.keySet.asScala) {
      str += location + " "
      temp = LocationRSS_HashMap.get(location)
      for (i <- 0 until temp.size) {
        str += temp.get(i) + " "
      }
      str += "\n"
    }
    str
  }
}
