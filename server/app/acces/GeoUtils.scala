package acces

import breeze.linalg.{DenseMatrix, DenseVector, linspace}

import scala.math._
import java.util.{ArrayList, List}
import utils.{GeoJSONMultiPoint, GeoPoint}
object GeoUtils {

  val R_eq: Double = 6356.752
  val R_pl: Double = 6371.001

  def radius(lat: Double): Double = {
    if (lat > Pi / 2.0 || lat < -Pi / 2.0) {
      throw new IllegalArgumentException("Most probably, lat is in degrees, must be in radians")
    }
    val r1 = R_eq
    val r2 = R_pl
    val sinB = sin(lat)
    val cosB = cos(lat)
    val radius = sqrt( (pow(pow(r1, 2.0)*cosB, 2.0) + pow(pow(r2, 2.0)*sinB, 2.0)) / (pow(r1*cosB, 2.0) + pow(r2*sinB, 2.0 )))
    radius
  }

  def dist(latlng_deg_1: DenseVector[Double], latlng_deg_2: DenseVector[Double]): Double = {
    if (latlng_deg_1.length != 2 || latlng_deg_2.length != 2) {
      throw new IllegalArgumentException("Incorrect latlng sizes")
    }
    val (lat1, lng1) = (toRadians(latlng_deg_1(0)), toRadians(latlng_deg_1(1)))
    val (lat2, lng2) = (toRadians(latlng_deg_2(0)), toRadians(latlng_deg_2(1)))

    val a = pow(sin((lat1 - lat2)/2.0), 2.0) + cos(lat1)*cos(lat2)*pow(sin((lng1-lng2)/2.0), 2.0)
    val c = 2.0*atan2( sqrt(a), sqrt(1.0-a) )
    val d = radius((lat1 + lat2)/2.0)*c
    return d*1000.0
  }

  def latlng2xy(latlng_deg: DenseVector[Double],
                latlng_deg_bl: DenseVector[Double],
                latlng_deg_ur: DenseVector[Double]): DenseVector[Double] = {
    if (latlng_deg.length != 2 || latlng_deg_bl.length != 2 || latlng_deg_ur.length != 2) {
      throw new IllegalArgumentException("Incorrect latlng sizes")
    }
    val latlng_deg_left = DenseVector[Double](
      (latlng_deg_bl(0) + latlng_deg_ur(0)) / 2.0,
      latlng_deg_bl(1)
    )
    val latlng_deg_right = DenseVector[Double](
      (latlng_deg_bl(0) + latlng_deg_ur(0)) / 2.0,
      latlng_deg_ur(1)
    )
    val latlng_deg_bottom = DenseVector[Double](
      latlng_deg_bl(0),
      (latlng_deg_bl(1) + latlng_deg_ur(1)) / 2.0
    )
    val latlng_deg_top = DenseVector[Double](
      latlng_deg_ur(0),
      (latlng_deg_bl(1) + latlng_deg_ur(1)) / 2.0
    )
    val width = dist(latlng_deg_left, latlng_deg_right)
    val height = dist(latlng_deg_bottom, latlng_deg_top)

    //Latitude of point, longitude of left
    val latlng_deg_x = DenseVector[Double](latlng_deg(0), latlng_deg_bl(1))
    val d_x = dist(latlng_deg_x, latlng_deg) * {if (latlng_deg(1) < latlng_deg_x(1)) -1.0 else 1.0}

    //Longitude of point, latitude of bottom
    val latlng_deg_y = DenseVector[Double](latlng_deg_bl(0), latlng_deg(1))
    val d_y = dist(latlng_deg_y, latlng_deg) * {if (latlng_deg(0) < latlng_deg_y(0)) -1.0 else 1.0}

    DenseVector[Double](d_x, d_y)
  }

  def latlng2xy(point: GeoPoint,
                bl: GeoPoint,
                ur: GeoPoint): DenseVector[Double] = {
    val latlng_deg = DenseVector[Double](point.dlat, point.dlon)
    val latlng_deg_bl = DenseVector[Double](bl.dlat, bl.dlon)
    val latlng_deg_ur = DenseVector[Double](ur.dlat, ur.dlon)
    latlng2xy(latlng_deg, latlng_deg_bl, latlng_deg_ur)
  }

  def latlng2xy(latlng_deg: DenseMatrix[Double],
                latlng_deg_bl: DenseVector[Double],
                latlng_deg_ur: DenseVector[Double]): DenseMatrix[Double] = {
    if (latlng_deg.cols != 2 || latlng_deg_bl.length != 2 || latlng_deg_ur.length != 2) {
      throw new IllegalArgumentException("Incorrect latlng sizes")
    }
    val xy: DenseMatrix[Double] = DenseMatrix.zeros[Double](latlng_deg.rows, latlng_deg.cols)
    for (i <- 0 to xy.rows - 1) {
      xy(i, ::) := latlng2xy(latlng_deg(i, ::).t, latlng_deg_bl, latlng_deg_ur).t
    }
    xy
  }

  def latlng2xy(points: GeoJSONMultiPoint, bl: GeoPoint, ur: GeoPoint): DenseMatrix[Double] = {
    val n = points.points.size()
    val latlng_deg = DenseMatrix.zeros[Double](n, 2)
    for (i <- 0 to n - 1) {
      val point = points.points.get(i)
      latlng_deg(i, ::) := DenseVector[Double](point.dlat, point.dlon).t
    }
    val latlng_deg_bl = DenseVector[Double](bl.dlat, bl.dlon)
    val latlng_deg_ur = DenseVector[Double](ur.dlat, ur.dlon)
    latlng2xy(latlng_deg, latlng_deg_bl, latlng_deg_ur)
  }


  def xy2latlng(xy: DenseVector[Double],
                latlng_deg_bl: DenseVector[Double],
                latlng_deg_ur: DenseVector[Double]): DenseVector[Double] = {
    if (xy.length != 2 || latlng_deg_bl.length != 2 || latlng_deg_ur.length != 2) {
      throw new IllegalArgumentException("Incorrect latlng or xy sizes")
    }
    val latlng_deg_left = DenseVector[Double](
      (latlng_deg_bl(0) + latlng_deg_ur(0)) / 2.0,
      latlng_deg_bl(1)
    )
    val latlng_deg_right = DenseVector[Double](
      (latlng_deg_bl(0) + latlng_deg_ur(0)) / 2.0,
      latlng_deg_ur(1)
    )
    val latlng_deg_bottom = DenseVector[Double](
      latlng_deg_bl(0),
      (latlng_deg_bl(1) + latlng_deg_ur(1)) / 2.0
    )
    val latlng_deg_top = DenseVector[Double](
      latlng_deg_ur(0),
      (latlng_deg_bl(1) + latlng_deg_ur(1)) / 2.0
    )
    val width = dist(latlng_deg_left, latlng_deg_right)
    val height = dist(latlng_deg_bottom, latlng_deg_top)

    val (x, y) = (xy(0), xy(1))
    val lat = latlng_deg_bl(0) + y / height * (latlng_deg_top(0) - latlng_deg_bottom(0))
    val lng = latlng_deg_bl(1) + x / width * (latlng_deg_right(1) - latlng_deg_left(1))

    DenseVector[Double](lat, lng)
  }

  def xy2latlng(xy: DenseVector[Double],
                bl: GeoPoint,
                ur: GeoPoint): DenseVector[Double] = {
    xy2latlng(xy,
      latlng_deg_bl = DenseVector[Double](bl.dlat, bl.dlon),
      latlng_deg_ur = DenseVector[Double](ur.dlat, ur.dlon))
  }

  def xy2latlng(xy: DenseMatrix[Double],
                latlng_deg_bl: DenseVector[Double],
                latlng_deg_ur: DenseVector[Double]): DenseMatrix[Double] = {
    if (xy.cols != 2 || latlng_deg_bl.length != 2 || latlng_deg_ur.length != 2) {
      throw new IllegalArgumentException("Incorrect latlng sizes")
    }
    val latlng = DenseMatrix.zeros[Double](xy.rows, xy.cols)
    for (i <- 0 to latlng.rows - 1) {
      latlng(i, ::) := xy2latlng(xy(i, ::).t, latlng_deg_bl, latlng_deg_ur).t
    }
    latlng
  }

  def xy2latlng(xy: DenseMatrix[Double],
                bl: GeoPoint,
                ur: GeoPoint): DenseMatrix[Double] = {
    xy2latlng(xy,
      latlng_deg_bl = DenseVector[Double](bl.dlat, bl.dlon),
      latlng_deg_ur = DenseVector[Double](ur.dlat, ur.dlon))
  }

  def dv2GeoPoint(latlng: DenseVector[Double]): GeoPoint = {
    if (latlng.length != 2) {
      throw new IllegalArgumentException("Incorrect latlng sizes")
    }
    return new GeoPoint(lat=latlng(0), lon=latlng(1))
  }

  def dm2GeoJSONMultiPoint(latlng: DenseMatrix[Double]): GeoJSONMultiPoint = {
    if (latlng.cols != 2) {
      throw new IllegalArgumentException("Incorrect latlng sizes")
    }
    val n = latlng.rows
    val geoJSONMultiPoint = new GeoJSONMultiPoint()
    for (i <- 0 to n - 1) {
      val point = new GeoPoint(lat=latlng(i, 0), lon=latlng(i, 1))
      geoJSONMultiPoint.points.add(point)
    }
    geoJSONMultiPoint
  }

  def grid_2D(bl: DenseVector[Double], ur: DenseVector[Double], h: Double): DenseMatrix[Double] = {
    assert(h > 0.0)
    assert(bl.length == ur.length && bl.length == 2)
    val Nx = scala.math.max( 1 + scala.math.ceil( (ur(0) - bl(0)) / h ).toInt, 2)
    val Ny = scala.math.max( 1 + scala.math.ceil( (ur(1) - bl(1)) / h ).toInt, 2)
    val lin_x = linspace(bl(0), ur(0), Nx)
    val lin_y = linspace(bl(1), ur(1), Ny)
    val grid = DenseMatrix.zeros[Double](Nx*Ny, 2)
    for (i <- 0 until Nx) {
      for (j <- 0 until Ny) {
        val k = Ny*i + j
        grid(k, ::) := DenseVector[Double](lin_x(i), lin_y(j)).t
      }
    }
    return grid
  }

}
