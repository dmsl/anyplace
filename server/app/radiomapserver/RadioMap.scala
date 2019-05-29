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

import Jama.Matrix
import java.io._
import java.text.DecimalFormat
import java.util
import java.util.{ArrayList, HashMap, LinkedList}

//remove if not needed
import scala.collection.JavaConversions._


object RadioMap {

  def authenticateRSSlogFileAndReturnBuildingsFloors(inFile: File): HashMap[String, LinkedList[String]] = {
    var line_num = 0
    var reader: BufferedReader = null
    val buildingsFloors = new HashMap[String, LinkedList[String]]()
    try {
      var line: String = null
      val fr = new FileReader(inFile)
      reader = new BufferedReader(fr)
      while ( {
        line = reader.readLine
        line != null
      }) {
        line_num += 1
        if (!(line.startsWith("#") || line.trim().isEmpty)) {
          line = line.replace(", ", " ")
          val temp = line.split(" ")
          if (temp.length != 8) {
            throw new Exception("Line " + line_num + " length is not equal to 8.")
          }
          java.lang.Float.parseFloat(temp(1))
          java.lang.Float.parseFloat(temp(2))
          java.lang.Float.parseFloat(temp(3))
          if (!temp(4).matches("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}")) {
            throw new Exception("Line " + line_num + " MAC Address is not valid.")
          }
          java.lang.Integer.parseInt(temp(5))
          java.lang.Integer.parseInt(temp(6))
          if (!buildingsFloors.containsKey(temp(7))) {
            val tempList = new LinkedList[String]()
            tempList.add(temp(6))
            buildingsFloors.put(temp(7), tempList)
          }
        }
      }
      fr.close()
      reader.close()
    } catch {
      case nfe: NumberFormatException => {
        System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath +
          ": Line " +
          line_num +
          " " +
          nfe.getMessage)
        return null
      }
      case e: Exception => {
        System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath +
          ": " +
          e.getMessage)
        return null
      }
    }
    buildingsFloors
  }


  class RadioMap(private val rss_folder: File,
                 private val radiomap_filename: String,
                 private val radiomap_AP: String,
                 private val defaultNaNValue: Int) {

    private val  RadioMap = new HashMap[String, HashMap[String, ArrayList[Integer]]]()

    private var NewRadioMap: HashMap[String, HashMap[Integer, ArrayList[Any]]] = new HashMap[String, HashMap[Integer, ArrayList[Any]]]()

    private var orientationLists: HashMap[Integer, ArrayList[Any]] = _

    private val isIndoor = this.radiomap_filename.contains("indoor")

    private val radiomap_mean_filename = radiomap_filename.replace(".", "-mean.")

    private val radiomap_rbf_weights_filename = radiomap_filename.replace(".", "-weights.")

    private val radiomap_parameters_filename = radiomap_filename.replace(".", "-parameters.")

    private var S_RBF: Float = -1f

    private var MIN_RSS: Int = java.lang.Integer.MAX_VALUE

    private var MAX_RSS: Int = java.lang.Integer.MIN_VALUE

    def createRadioMap(): Boolean = {
      if (!rss_folder.exists() || !rss_folder.isDirectory) {

        return false
      }
      RadioMap.clear()
      createRadioMapFromPath(rss_folder)
      if (!writeRadioMap()) {
        println("herelsolea01")
        return false
      }
      true
    }

    private def createRadioMapFromPath(inFile: File) {
      if (inFile.exists()) {
        if (inFile.canExecute() && inFile.isDirectory) {
          val list = inFile.list()
          if (list != null) {
            for (i <- 0 until list.length) {
              createRadioMapFromPath(new File(inFile, list(i)))
            }
          }
        } else if (inFile.canRead() && inFile.isFile) {
          parseLogFileToRadioMap(inFile)
        }
      }
    }

    def parseLogFileToRadioMap(inFile: File) {
      var MACAddressMap: HashMap[String, ArrayList[Integer]] = null
      var RSS_Values: ArrayList[Integer] = null
      val MAG_Values: ArrayList[ArrayList[Float]] = null
      var orientationList: ArrayList[Any] = null
      val f = inFile
      var reader: BufferedReader = null
      if (!authenticateRSSlogFile(f)) {
        return
      }
      var group = 0
      var line_num = 0
      try {
        var line: String = null
        var key = ""
        val degrees = 360
        val num_orientations = 4
        val range = degrees / num_orientations
        val deviation = range / 2
        var RSS_Value = 0
        val fr = new FileReader(f)
        reader = new BufferedReader(fr)
        while ( {
          line = reader.readLine;
          line != null
        }) {
          line_num += 1
          if (!(line.startsWith("#") || line.trim().isEmpty)) {

            line = line.replace(", ", " ")
            val temp = line.split(" ")
            RSS_Value = java.lang.Integer.parseInt(temp(5))
            key = temp(1) + ", " + temp(2)
            group = (((Math.round((java.lang.Float.parseFloat(temp(3))) + deviation) %
              degrees) /
              range) %
              num_orientations).toInt
            orientationLists = NewRadioMap.get(key)
            if (orientationLists == null) {
              orientationLists = new HashMap[Integer, ArrayList[Any]](Math.round(num_orientations))
              orientationList = new ArrayList[Any](2)
              orientationLists.put(group, orientationList)
              MACAddressMap = new HashMap[String, ArrayList[Integer]]()
              RSS_Values = new ArrayList[Integer]()
              RSS_Values.add(RSS_Value)
              MACAddressMap.put(temp(4).toLowerCase(), RSS_Values)
              orientationList.add(MACAddressMap)
              orientationList.add(0)
              NewRadioMap.put(key, orientationLists)
            } else if (orientationLists.get(group) == null) {
              orientationList = new ArrayList[Any](2)
              orientationLists.put(group, orientationList)
              MACAddressMap = new HashMap[String, ArrayList[Integer]]()
              RSS_Values = new ArrayList[Integer]()
              RSS_Values.add(RSS_Value)
              MACAddressMap.put(temp(4).toLowerCase(), RSS_Values)
              orientationList.add(MACAddressMap)
              orientationList.add(0)
              NewRadioMap.put(key, orientationLists)
            } else {
              MACAddressMap = orientationLists.get(group).get(0).asInstanceOf[HashMap[String, ArrayList[Integer]]]
              RSS_Values = MACAddressMap.get(temp(4).toLowerCase())
              var position = orientationLists.get(group).get(1).asInstanceOf[java.lang.Integer]
              if (RSS_Values == null) {
                RSS_Values = new ArrayList[Integer]()
              }
              if (position == RSS_Values.size) {
                position = position + 1
                orientationLists.get(group).set(1, position)
                RSS_Values.add(RSS_Value)
                MACAddressMap.put(temp(4).toLowerCase(), RSS_Values)
              } else {
                for (i <- RSS_Values.size until position - 1) {
                  RSS_Values.add(this.defaultNaNValue)
                }
                RSS_Values.add(RSS_Value)
                MACAddressMap.put(temp(4).toLowerCase(), RSS_Values)
              }
            }
          }
        }
        fr.close()
        reader.close()
      } catch {
        case e: Exception => System.err.println("Error while parsing RSS log file " + f.getAbsolutePath +
          ": " +
          e.getMessage)
      }
    }


    def authenticateRSSlogFile(inFile: File): Boolean = {
      var line_num = 0
      var reader: BufferedReader = null
      try {
        var line: String = null
        val fr = new FileReader(inFile)
        reader = new BufferedReader(fr)
        while ( {
          line = reader.readLine
          line != null
        }) {

          line_num += 1
          // Check X, Y or Latitude, Longitude
          if (!(line.startsWith("#") || line.trim().isEmpty)) {
            // Remove commas
            line = line.replace(", ", " ")
            val temp = line.split(" ")
            java.lang.Float.parseFloat(temp(1))
            java.lang.Float.parseFloat(temp(2))
            java.lang.Float.parseFloat(temp(3))
            if (!temp(4).matches("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}")) {
              throw new Exception("Line " + line_num + " MAC Address is not valid.")
            }
            java.lang.Integer.parseInt(temp(5))
            java.lang.Integer.parseInt(temp(6))
          }
        }
        fr.close()
        reader.close()
      } catch {
        case nfe: NumberFormatException => {
          System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath +
            ": Line " +
            line_num +
            " " +
            nfe.getMessage)
          return false
        }
        case e: Exception => {
          System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath +
            ": " +
            e.getMessage)
          return false
        }
      }
      true
    }



    def writeParameters(orientations: Int): Boolean = {
      for (i <- 0 until orientations) {
        val group = i * 360 / orientations
        var fos: FileOutputStream = null
        val RM = new RadioMapMean(this.isIndoor, this.defaultNaNValue)
        if (!RM.ConstructRadioMap(new File(radiomap_mean_filename))) {
          return false
        }
        if (!find_MIN_MAX_Values(group)) {
          return false
        }
        System.out.print("Calculating RBF parameter...")
        S_RBF = calculateSGreek(RM, group)
        if (S_RBF == -1) {
          return false
        }
        System.out.print("Done!")
        val radiomap_parameters_file = new File(radiomap_parameters_filename)
        try {
          fos = if (i == 0) new FileOutputStream(radiomap_parameters_file, false) else new FileOutputStream(radiomap_parameters_file,
            true)
        } catch {
          case e: Exception => {
            System.err.println("Error while writing parameters: " + e.getMessage)
            radiomap_parameters_file.delete()
            return false
          }
        }
        try {
          if (i != 0) {
            fos.write("\n".getBytes)
          } else {
            fos.write(("NaN, " + this.defaultNaNValue + "\n").getBytes)
          }
          fos.write(("RBF, " + group + ", " + this.S_RBF).getBytes)
          fos.close()
        } catch {
          case e: Exception => {
            System.err.println("Error while writing parameters: " + e.getMessage)
            radiomap_parameters_file.delete()
            return false
          }
        }
      }
      println("Written Parameters!")
      true
    }

    private def find_MIN_MAX_Values(group: Int): Boolean = {
      var frRadiomap: FileReader = null
      var readerRadiomap: BufferedReader = null
      try {
        var radiomapLine: String = null
        var temp: Array[String] = null
        frRadiomap = new FileReader(radiomap_filename)
        readerRadiomap = new BufferedReader(frRadiomap)
        while ( {
          radiomapLine = readerRadiomap.readLine;
          radiomapLine != null
        }) {
          if (!(radiomapLine.startsWith("#") || radiomapLine.trim().isEmpty)) {
            radiomapLine = radiomapLine.replace(", ", " ")
            temp = radiomapLine.split(" ")
            if (temp(0).trim().equals("#")) {
              if (!temp(1).trim().equals("NaN")) {
                // Must have more than 3 fields
                if (temp.length < 5) {
                  return false
                } else if (this.isIndoor &&
                  (!temp(1).trim().equalsIgnoreCase("X") || !temp(2).trim().equalsIgnoreCase("Y"))) {
                  return false
                }
              }
            } else {
              if (temp.length < 4) {
                return false
              }
              if (java.lang.Integer.parseInt(temp(2)) == group) {
                for (i <- 3 until temp.length) {
                  set_MIN_MAX_RSS(java.lang.Integer.parseInt(temp(i)))
                }
              }
            }
          }
        }
        frRadiomap.close()
        readerRadiomap.close()
      }

      catch {
        case e: Exception => {
          System.err.println("Error while finding min and max RSS values: " + e.getMessage)
          return false
        }
      }
      true
    }

    private def set_MIN_MAX_RSS(RSS_Value: Int) {
      if (MIN_RSS > RSS_Value && RSS_Value != this.defaultNaNValue) {
        MIN_RSS = RSS_Value
      }
      if (MAX_RSS < RSS_Value) {
        MAX_RSS = RSS_Value
      }
    }

    private def calculateSGreek(RM: RadioMapMean, group: Int): java.lang.Float = {
      if (RM == null) {
        return -1f
      }
      var maximumDistance = 0.0f
      val allDistances= new util.ArrayList[Float]()
      var result: Float = 0.0f
      for (i <- 0 until RM.getOrderList().size; j <- i + 1 until RM.getOrderList().size) {
        val RadioMapFile = RM.getLocationRSS_HashMap(group)
        if (RadioMapFile != null && RadioMapFile.get(RM.getOrderList.get(i)) != null &&
          RadioMapFile.get(RM.getOrderList().get(j)) != null) {
          result = calculateEuclideanDistance(RadioMapFile.get(RM.getOrderList().get(i)), RadioMapFile.get(RM.getOrderList().get(j)))
          if (result == java.lang.Float.NEGATIVE_INFINITY) {
            return -1f
          }
          allDistances.add(result)
        }
      }
      maximumDistance = allDistances.max
      java.lang.Float.valueOf((maximumDistance / Math.sqrt(2 * RM.getOrderList.size)).toFloat)
    }

    private def calculateEuclideanDistance(real: String, estimate: String): Double

    = {
      var pos_error: Double = 0.0
      var temp_real: Array[String] = null
      var temp_estimate: Array[String] = null
      var x1: Double = 0.0
      var x2: Double = 0.0
      temp_real = real.split(" ")
      temp_estimate = estimate.split(" ")
      try {
        x1 = Math.pow(java.lang.Double.parseDouble(temp_real(0)) - java.lang.Double.parseDouble(temp_estimate(0)),
          2)
        x2 = Math.pow(java.lang.Double.parseDouble(temp_real(1)) - java.lang.Double.parseDouble(temp_estimate(1)),
          2)
      } catch {
        case e: Exception => {
          System.err.println("Error while calculating Euclidean distance: " + e.getMessage)
          return -1
        }
      }
      pos_error = Math.sqrt((x1 + x2))
      pos_error
    }

    private def writeRadioMap(): Boolean

    = {
      println("writing radio map to files")
      val dec = new DecimalFormat("###.#")
      var MACAddressMap: HashMap[String, ArrayList[Integer]] = null
      val RSS_Values: ArrayList[Integer] = null
      val AP = new ArrayList[String]()
      var fos: FileOutputStream = null
      var fos_mean: FileOutputStream = null
      val out: String = null
      val orientations = 4
      val radiomap_file = new File(radiomap_filename)
      val radiomap_mean_file = new File(radiomap_mean_filename)
      if (NewRadioMap.isEmpty) {
        return false
      }
      val f = new File(radiomap_AP)
      var reader: BufferedReader = null
      if (f.exists()) {
        try {
          var line: String = null
          val fr = new FileReader(f)
          reader = new BufferedReader(fr)
          while ( {
            line = reader.readLine;
            line != null
          }) {
            AP.add(line.toLowerCase())
          }
          fr.close()
          reader.close()
        } catch {
          case e: Exception => System.err.println("Error while parsing AP txt file " + f.getAbsolutePath +
            ": " +
            e.getMessage)
        }
      }
      try {
        fos = new FileOutputStream(radiomap_file, false)
        fos_mean = new FileOutputStream(radiomap_mean_file, false)
      } catch {
        case e: FileNotFoundException => {
          System.err.println("Error while writing radio map: " + e.getMessage)
          radiomap_file.delete()
          radiomap_mean_file.delete()
          return false
        }
      }
      try {
        var count = 0
        var max_values = 0
        var first = 0
        val NaNValue = "# NaN " + this.defaultNaNValue + "\n"
        val header = "# X, Y, HEADING, "
        fos.write(NaNValue.getBytes)
        fos_mean.write(NaNValue.getBytes)
        fos.write(header.getBytes)
        fos_mean.write(header.getBytes)
        val MACKeys = new ArrayList[String]()
        var MRSS_Values: Array[Int] = null
        var heading = 0
        var x_y = ""
        var group = 0
        for ((key, value) <- NewRadioMap) {
          val degrees = 360
          for ((key, value) <- value) {
            MACAddressMap = value.get(0).asInstanceOf[HashMap[String, ArrayList[Integer]]]
            for ((key, value) <- MACAddressMap) {
              val MACAddress = key
              if (!MACKeys.contains(MACAddress.toLowerCase())) {
                if (AP.size == 0 || AP.contains(MACAddress.toLowerCase())) {
                  MACKeys.add(MACAddress.toLowerCase())
                  if (first == 0) {
                    fos.write((MACAddress.toLowerCase()).getBytes)
                    fos_mean.write((MACAddress.toLowerCase()).getBytes)
                  } else {
                    fos.write((", " + MACAddress.toLowerCase()).getBytes)
                    fos_mean.write((", " + MACAddress.toLowerCase()).getBytes)
                  }
                  first += 1
                }
              }
            }
          }
        }
        for ((key, value) <- NewRadioMap) {
          val degrees = 360
          group = degrees / orientations
          x_y = key
          for ((key, value) <- value) {
            max_values = 0
            heading = key * group
            MACAddressMap = value.get(0).asInstanceOf[HashMap[String, ArrayList[Integer]]]
            for ((key, value) <- MACAddressMap) {
              val wifi_rss_values = value
              if (wifi_rss_values.size > max_values) {
                max_values = wifi_rss_values.size
              }
            }
            if (count == 0) {
              fos.write(("\n").getBytes)
              fos_mean.write(("\n").getBytes)
            }
            MRSS_Values =new Array[Int](MACKeys.size)
            for (v <- 0 until max_values) {
              fos.write((x_y + ", " + heading).getBytes)
              for (i <- 0 until MACKeys.size) {
                var rss_value = 0
                if (MACAddressMap.containsKey(MACKeys.get(i).toLowerCase())) {
                  if (v >=
                    MACAddressMap.get(MACKeys.get(i).toLowerCase()).size &&
                    MACAddressMap.get(MACKeys.get(i).toLowerCase()).size <
                      max_values) {
                    MRSS_Values(i) += this.defaultNaNValue
                    rss_value = this.defaultNaNValue
                  } else {
                    rss_value = MACAddressMap.get(MACKeys.get(i).toLowerCase()).get(v)
                    MRSS_Values(i) += rss_value
                  }
                } else {
                  rss_value = this.defaultNaNValue
                  MRSS_Values(i) += this.defaultNaNValue
                }
                fos.write((", " + dec.format(rss_value)).getBytes)
              }
              fos.write("\n".getBytes)
            }
            fos_mean.write((x_y + ", " + heading).getBytes)
            for (i <- MRSS_Values.indices) {
              fos_mean.write((", " + dec.format(MRSS_Values(i).toFloat / max_values))
                .getBytes)
            }
            fos_mean.write("\n".getBytes)
            count += 1
          }
        }
        if (!writeParameters(orientations)) {
          return false
        }
        if (!writeRBFWeights(orientations)) {
        }
        fos.close()
      } catch {
        case cce: ClassCastException => {
          System.err.println("Error1: " + cce.getMessage)
          return false
        }
        case nfe: NumberFormatException => {
          System.err.println("Error2: " + nfe.getMessage)
          return false
        }
        case fnfe: FileNotFoundException => {
          System.err.println("Error3: " + fnfe.getMessage)
          return false
        }
        case ioe: IOException => {
          System.err.println("Error4: " + ioe.getMessage)
          return false
        }
        case e: Exception => {
          System.err.println("Error5: " + e.getMessage)
          return false
        }
      }
      println("Finished writing radio map to files!")
      true
    }

    def writeRBFWeights(orientation: Int): Boolean = {
      val start = System.currentTimeMillis()
      println("Writing RBF weights!")
      for (x <- 0 until orientation) {
        val group = x * 360 / orientation
        val MatrixU = create_U_matrix(group)
        if (MatrixU == null) {
          return false
        }
        println("created U matrix! time[ " + (System.currentTimeMillis() - start) +
          "] ms")
        val Matrixd = create_d_matrix(MatrixU.getRowDimension, group)
        if (Matrixd == null) {
          return false
        }
        println("created matrix d! time[ " + (System.currentTimeMillis() - start) +
          "] ms")
        val UPlus = computeUPlusMatrix(MatrixU)
        if (UPlus == null) {
          return false
        }
        println("computed Plus matrix! time[ " + (System.currentTimeMillis() - start) +
          "] ms")
        val w = computeWMatrix(UPlus, Matrixd)
        if (w == null) {
          return false
        }
        println("computed W matrix! time[ " + (System.currentTimeMillis() - start) +
          "] ms")
        val dec = new DecimalFormat("###.########")
        var fos: FileOutputStream = null
        val radiomap_rbf_weights_file = new File(radiomap_rbf_weights_filename)
        try {
          fos = if (x == 0) new FileOutputStream(radiomap_rbf_weights_file, false) else new FileOutputStream(radiomap_rbf_weights_file,
            true)
        } catch {
          case e: FileNotFoundException => {
            System.err.println("Error RBF weights: " + e.getMessage)
            radiomap_rbf_weights_file.delete()
            return false
          }
        }
        try {
          if (x == 0) fos.write("# Heading wx, wy\n".getBytes)
          for (i <- 0 until w.getRowDimension) {
            fos.write((group + ", ").getBytes)
            for (j <- 0 until w.getColumnDimension) {
              fos.write(dec.format(w.get(i, j)).getBytes)
              if (j != w.getColumnDimension - 1) {
                fos.write(", ".getBytes)
              }
            }
            fos.write("\n".getBytes)
          }
          fos.close()
        } catch {
          case e: Exception => {
            System.err.println("Error RBF weights: " + e.getMessage)
            radiomap_rbf_weights_file.delete()
            return false
          }
        }
      }
      println("Written RBF weights! time[ " + (System.currentTimeMillis() - start) +
        "] ms")
      true
    }

    private def create_U_matrix(orientation: Int): Matrix

    = {
      var UArray: ArrayList[ArrayList[Double]] = null
      var CArray: ArrayList[ArrayList[Double]] = null
      var Urow: ArrayList[Double] = null
      CArray = fillCArray(orientation)
      if (CArray == null) {
        return null
      }
      UArray = fillUArray(CArray, orientation)
      if (UArray == null) {
        return null
      }
      val row = UArray.size
      val column = CArray.size
      val UArrayMatrix = Array.ofDim[Double](row, column)
      for (i <- 0 until row) {
        Urow = UArray.get(i)
        for (j <- 0 until column) {
          UArrayMatrix(i)(j) = Urow.get(j)
        }
      }
      new Matrix(UArrayMatrix)
    }

    private def fillCArray(orientation: Int): ArrayList[ArrayList[Double]]

    = {
      var CArray = new ArrayList[ArrayList[Double]]()
      var CArrayLine: ArrayList[Double] = null
      var temp: Array[String] = null
      var frRadiomapMean: FileReader = null
      var readerRadiomapMean: BufferedReader = null
      try {
        frRadiomapMean = new FileReader(radiomap_mean_filename)
        readerRadiomapMean = new BufferedReader(frRadiomapMean)
        var line: String = null
        while ( {
          line = readerRadiomapMean.readLine;
          line != null
        }) {
          if (!line.trim().isEmpty) {
            line = line.replace(", ", " ")
            temp = line.split(" ")
            if (temp(0).trim().equals("#")) {
              if (!temp(1).trim().equals("NaN")) {
                if (temp.length < 5) {
                  return null
                } else if (this.isIndoor &&
                  (!temp(1).trim().equalsIgnoreCase("X") || !temp(2).trim().equalsIgnoreCase("Y"))) {
                  return null
                }
              }
            } else {
              if (temp.length < 4) {
                return null
              }
              CArrayLine = new ArrayList[Double]()
              if (java.lang.Integer.parseInt(temp(2)) == orientation) {
                for (i <- 3 until temp.length) {
                  CArrayLine.add(java.lang.Double.parseDouble(temp(i)))
                }
                CArray.add(CArrayLine)
              }
            }
          }
        }
        frRadiomapMean.close()
        readerRadiomapMean.close()
      }

      catch {
        case e: Exception => {
          System.err.println("Error while populating C array of RBF: " + e.getMessage)
          CArray = null
        }
      }
      CArray
    }

    private def fillUArray(CArray: ArrayList[ArrayList[Double]], orientation: Int): ArrayList[ArrayList[Double]]

    = {
      val UArray = new ArrayList[ArrayList[Double]]()
      var frRadiomap: FileReader = null
      var readerRadiomap: BufferedReader = null
      var Srow: ArrayList[Double] = null
      var Crow: ArrayList[Double] = null
      var Urow: ArrayList[Double] = null
      var temp: Array[String] = null
      try {
        var radiomapLine: String = null
        frRadiomap = new FileReader(radiomap_filename)
        readerRadiomap = new BufferedReader(frRadiomap)
        while ( {
          radiomapLine = readerRadiomap.readLine;
          radiomapLine != null
        }) {
          if (!radiomapLine.trim().isEmpty) {
            radiomapLine = radiomapLine.replace(", ", " ")
            temp = radiomapLine.split(" ")
            if (temp(0).trim().equals("#")) {
              if (!temp(1).trim().equals("NaN")) {

                if (temp.length < 5) {
                  return null
                } else if (this.isIndoor &&
                  (!temp(1).trim().equalsIgnoreCase("X") || !temp(2).trim().equalsIgnoreCase("Y"))) {
                  return null
                }
              }
            }
            else {
              if (temp.length < 4) {
                return null
              }
              if (java.lang.Integer.parseInt(temp(2)) == orientation) {
                Srow = new ArrayList[Double]()
                for (i <- 3 until temp.length) {
                  Srow.add(java.lang.Double.parseDouble(temp(i)))
                  set_MIN_MAX_RSS(java.lang.Integer.parseInt(temp(i)))
                }
                Urow = new ArrayList[Double]()
                for (i <- 0 until CArray.size) {
                  Crow = CArray.get(i)
                  val numerator = computeNumerator(Srow, Crow)
                  val denominator = computeDenominator(Srow, CArray)
                  Urow.add(numerator / denominator)
                }
                UArray.add(Urow)
              }
            }
          }
        }
        frRadiomap.close()
        readerRadiomap.close()
      } catch {
        case e: Exception => {
          System.err.println("Error while populating U array of RBF: " + e.getMessage)
          return null
        }
      }
      UArray
    }

    private def computeNumerator(Srow: ArrayList[Double], Crow: ArrayList[Double]): java.lang.Double

    = {
      var Si: java.lang.Double = null
      var Cj: java.lang.Double = null
      val firstParameter = -1 / (2 * Math.pow(S_RBF, 2))
      var secondParameter = 0.0d
      for (i <- 0 until Srow.size) {
        Si = Srow.get(i)
        Cj = Crow.get(i)
        secondParameter += Math.pow(Si - Cj, 2)
      }
      Math.exp(firstParameter * secondParameter)
    }

    private def computeDenominator(Srow: ArrayList[Double], C: ArrayList[ArrayList[Double]]): java.lang.Double

    = {
      var Crow: ArrayList[Double] = null
      var sum = 0.0d
      for (i <- 0 until C.size) {
        Crow = C.get(i)
        sum += computeNumerator(Srow, Crow)
      }
      sum
    }

    private def create_d_matrix(rowDimension: Int, orientation: Int): Matrix
    = {
      var frRadiomap: FileReader = null
      var readerRadiomap: BufferedReader = null
      var temp: Array[String] = null
      val row = rowDimension
      val column = 2
      val dArrayMatrix = Array.ofDim[Double](row, column)
      var i = 0
      try {
        var radiomapLine: String = null
        frRadiomap = new FileReader(radiomap_filename)
        readerRadiomap = new BufferedReader(frRadiomap)
        while ( {
          radiomapLine = readerRadiomap.readLine;
          radiomapLine != null
        }) {
          if (radiomapLine.trim().isEmpty) {
            radiomapLine = radiomapLine.replace(", ", " ")
            temp = radiomapLine.split(" ")
            if (temp(0).trim().equals("#")) {
              if (!temp(1).trim().equals("NaN")) {

                if (temp.length < 5) {
                  return null
                } else if (this.isIndoor &&
                  (!temp(1).trim().equalsIgnoreCase("X") || !temp(2).trim().equalsIgnoreCase("Y"))) {
                  return null
                }
              }
            } else {
              if (temp.length < 4) {
                return null
              }
              if (java.lang.Integer.parseInt(temp(2)) == orientation) {
                for (k <- 0 until 2) {
                  dArrayMatrix(i)(k) = java.lang.Double.parseDouble(temp(k))
                }
                i
              }
            }
          }
        }
        frRadiomap.close()
        readerRadiomap.close()
      } catch {
        case e: Exception => {
          System.err.println("Error while creating d matrix of RBF: " + e.getMessage)
          return null
        }
      }
      new Matrix(dArrayMatrix)
    }

    private def computeUPlusMatrix(MatrixU: Matrix): Matrix

    = {
      try {
        val Utranspose = MatrixU.transpose()
        val UtransposeU = Utranspose.times(MatrixU)
        val UtransposeUInverse = UtransposeU.inverse()
        UtransposeUInverse.times(Utranspose)
      } catch {
        case e: Exception => {
          System.err.println("Error while computing U+ matrix of RBF: " + e.getMessage)
          null
        }
      }
    }

    private def computeWMatrix(UPlus: Matrix, Matrixd: Matrix): Matrix

    = {
      var w: Matrix = null
      try {
        w = UPlus.times(Matrixd)
        w
      } catch {
        case e: Exception => {
          System.err.println("Error while computing w matrix of RBF: " + e.getMessage)
          null
        }
      }
    }


    private def calculateEuclideanDistance(l1: ArrayList[String], l2: ArrayList[String]): Float = {
      var finalResult = 0.0f
      var v1: Float = 0.0f
      var v2: Float = 0.0f
      var temp: Float = 0.0f
      var str: String = null
      for (i <- 0 until l1.size) {
        try {
          str = l1.get(i)
          v1 = java.lang.Float.valueOf(str.trim()).floatValue()
          str = l2.get(i)
          v2 = java.lang.Float.valueOf(str.trim()).floatValue()
        } catch {
          case e: Exception => return java.lang.Float.NEGATIVE_INFINITY
        }
        temp = v1 - v2
        temp *= temp
        finalResult += temp
      }
      Math.sqrt(finalResult).toFloat
    }

    private def printMatrix(m: Matrix) {
      for (i <- 0 until m.getRowDimension) {
        for (j <- 0 until m.getColumnDimension) {
          System.out.print(m.get(i, j) + " ")
        }
        println()
      }
    }
  }

}