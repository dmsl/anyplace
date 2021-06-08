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

import play.Logger
import java.text.NumberFormat
import java.text.ParseException
import java.util._

import datasources.SCHEMA
//remove if not needed
import scala.collection.JavaConversions._

object Dijkstra {

  class Graph {

    var vertices: List[DVertex] = new ArrayList[DVertex]()

    var hmp: HashMap[String, DVertex] = new HashMap[String, DVertex]()

    def addPois(pois: List[HashMap[String, String]]) {
      for (p <- pois) {
        val dv = new DVertex(p)
        this.vertices.add(dv)
        hmp.put(p.get(SCHEMA.fPuid), dv)
      }
    }

    def addPoi(p: HashMap[String, String]) {
      val dv = new DVertex(p)
      this.vertices.add(dv)
      hmp.put(p.get(SCHEMA.fPuid), dv)
    }

    def addEdges(conns: List[HashMap[String, String]]) {
      val nf = NumberFormat.getInstance(Locale.ENGLISH)
      var w = 0.0
      for (e <- conns) {
        try {
          val weight = e.getOrElse(SCHEMA.fWeight,null)
          if (weight != null)
            w = nf.parse(weight).doubleValue()
        } catch {
          case e1: ParseException =>
        }
        val a = hmp.getOrElse(e.get(SCHEMA.fPoisA), null)
        val b = hmp.getOrElse(e.get(SCHEMA.fPoisB), null)
        if (!(a == null || b == null)) {
          a.adjacencies.add(new DEdge(w, b))
          b.adjacencies.add(new DEdge(w, a))
        }
      }
    }

    def addEdge(conn: HashMap[String, String]) {
      val nf = NumberFormat.getInstance(Locale.ENGLISH)
      var w = 0.0
      try {
        w = nf.parse(conn.get(SCHEMA.fWeight)).doubleValue()
      } catch {
        case e1: ParseException =>
      }
      val a = hmp.getOrElse(conn.get(SCHEMA.fPoisA), null)
      val b = hmp.getOrElse(conn.get(SCHEMA.fPoisB), null)
      if (a == null || b == null) {
        return
      }
      a.adjacencies.add(new DEdge(w, b))
      b.adjacencies.add(new DEdge(w, a))
    }

    def getVertex(puid: String): DVertex = hmp.get(puid)
  }

  class DVertex(p: HashMap[String, String]) extends Comparable[Any] {

    var poi: HashMap[String, String] = p

    var previous: DVertex = null

    var minDistance: Double = java.lang.Double.POSITIVE_INFINITY

    var adjacencies: List[DEdge] = new ArrayList[DEdge]()

    var puid: String = this.poi.get(SCHEMA.fPuid)

    override def compareTo(o: Any): Int = {
      java.lang.Double.compare(this.minDistance, o.asInstanceOf[DVertex].minDistance)
    }
  }

  class DEdge(var weight: Double, dv: DVertex) {

    var target: DVertex = dv
  }

  private def computePath(graph: Graph, puid_from: String, puid_to: String): List[HashMap[String, String]] = {
    var dv = graph.getVertex(puid_from)
    if (dv == null) {
      return Collections.emptyList()
    }
    val visited = new HashSet[String]()
    dv.minDistance = 0.0
    val vqueue = new PriorityQueue[DVertex]()
    vqueue.add(dv)
    var tar: DVertex = null
    while (!vqueue.isEmpty) {
      dv = vqueue.poll()
      if (!visited.contains(dv.puid)) {
        //continue
        visited.add(dv.puid)
        if (dv.minDistance == java.lang.Double.POSITIVE_INFINITY) {
          return Collections.emptyList()
        }
        if (dv.puid.equalsIgnoreCase(puid_to)) {
          Logger.info("Path found!")
          return path(dv)
        }
        for (e <- dv.adjacencies) {
          tar = e.target
          val dist_no_tar = dv.minDistance + e.weight
          if (dist_no_tar < tar.minDistance) {
            tar.minDistance = dist_no_tar
            tar.previous = dv
            vqueue.add(tar)
          }
        }
      }
    }
    Collections.emptyList()
  }

  private def path(dv_in: DVertex): List[HashMap[String, String]] = {
    val final_path = new LinkedList[HashMap[String, String]]()
    var dv = dv_in
    final_path.offerFirst(dv.poi)
    while (dv.previous != null) {
      dv = dv.previous
      final_path.offerFirst(dv.poi)
    }
    final_path
  }

  def getShortestPath(graph: Graph, puid_from: String, puid_to: String): List[HashMap[String, String]] = {
    Logger.info("Dijkstra from[" + puid_from + "]")
    Logger.info("Dijkstra to[" + puid_to + "]")
    computePath(graph, puid_from, puid_to)
  }
}
