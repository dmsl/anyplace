package acces

import java.io.PrintWriter

import breeze.linalg.{*, DenseMatrix, DenseVector, argsort, diag, max, min, sum, svd}
import breeze.numerics.exp
import breeze.stats.meanAndVariance
import dk.gp.cov.CovSEiso
import dk.gp.gpr.{GprModel, gpr}

import scala.math.{log, pow, sqrt}
import scala.util.control.Breaks.{break, breakable}

/**
  * Calculates CRLB <=> ACCES score for measurement map using RBF kernel
  * Due to the nature of CRLB, score goes to infinity if measurements are collected in lines
  * or if supremum points are present. Lines are handled with pseudo-inverse, but supremums are not.
  * @param X - Matrix of independent variables, e.g., coordinates.
  *          Rows - different points (1, 2, ...). Columns - different coordinates (x, y, ...)
  * @param Y - Matrix of measurements, e.g., RSSI values.
  *          Rows - different зщштеы (1, 2, ...). Columns - different measurements (AP1, AP2, ...)
  * @param Ms - Not used.
  * @param sfs - multiplier in RBF kernel. Provide to use previously estimated parameters.
  * @param noises - additive noise of measurements. Provide to use previously estimated parameters.
  * @param gammas - multiplier in exponent in RBF kernel. Provide to use previously estimated parameters.
  * @param cut_k_features - whether to consider only k best features (e.g., APs).
  *                       Improves performance, but results could be different.
  * @param drop_redundant_features - whether to drop features with zero variance,
  *                                e.g., ignore distant Wi-Fi APs. Default - true.
  * @param X_min - minimum value of X from prior knowledge (not needed if normalize_x = false)
  * @param X_max - maximum value of X from prior knowledge (not needed if normalize_x = false)
  * @param Y_min - minimum values of Y. Recommended to specify for each particular component of Y
  *              as it will affect which features to drop if cut_k_features is provided.
  * @param Y_max - maximum values of Y
  * @param Y_means - fixed means of Y from prior knowledge
  * @param normalize_x - whether to normalize X (false recommended if X is coordinates). Default - false.
  * @param normalize_y - whether to normalize Y, i.e., subtract means before fitting (true recommended). Default - true.
  */
class AccesRBF(
                X: DenseMatrix[Double], Y: DenseMatrix[Double],
                Ms: Option[Array[DenseMatrix[Double]]] = None,
                sfs: Option[DenseVector[Double]] = None,
                noises: Option[DenseVector[Double]] = None,
                gammas: Option[DenseVector[Double]] = None,
                cut_k_features: Option[Int] = None,
                drop_redundant_features: Boolean = true,
                X_min: Option[DenseVector[Double]] = None,
                X_max: Option[DenseVector[Double]] = None,
                Y_min: Option[DenseVector[Double]] = None,
                Y_max: Option[DenseVector[Double]] = None,
                Y_means: Option[DenseVector[Double]] = None,
                normalize_x: Boolean = false,
                normalize_y: Boolean = true) {

  val _X_min: DenseVector[Double] = X_min.getOrElse(min(X(::, *)).t)
  val _X_max: DenseVector[Double] = X_max.getOrElse(max(X(::, *)).t)
  val _X_normed: DenseMatrix[Double] = if (normalize_x) {
    normalize_X(X=X, X_min=_X_min, X_max=_X_max)
  } else {
    X
  }
  //Attention: if use different sources, then y_min and y_max could be incorrect
  //if not provided in advance, e.g., for Wi-Fi could be [-110, 0], but for magnetic could be [30, 90]
  val _Y_min: DenseVector[Double] = Y_min.getOrElse(min(Y) * DenseVector.ones[Double](Y.cols))
  val _Y_max: DenseVector[Double] = Y_max.getOrElse(max(Y) * DenseVector.ones[Double](Y.cols))
  if (max(_Y_min) > min(Y) || min(_Y_max) < max(Y)) {
    throw new IllegalArgumentException("Invalid Y_max, Y_min values")
  }
  val _Y_means: DenseVector[Double] = if (normalize_y) {
    Y_means.getOrElse(get_column_means(A=Y))
  } else {
    DenseVector.zeros(Y.cols)
  }
  val _selected_features: IndexedSeq[Int] = select_features(Y=Y, Y_min=_Y_min, Y_max=_Y_max,
    cut_k_features.getOrElse(Y.cols), drop_redundant = drop_redundant_features)
  val _Y_normed = normalize_Y( Y=Y, Y_means=_Y_means)

//  LPLogger.debug("x_min", _X_min)
//  LPLogger.debug("x_max", _X_max)
//  LPLogger.debug("X_normed\n", _X_normed.toDenseVector)
//  LPLogger.debug("Y_min", _Y_min)
//  LPLogger.debug("Y_max", _Y_max)
//  LPLogger.debug("Y_means", _Y_means)
//  LPLogger.debug("Y_normed", _Y_normed.toDenseVector)

  var _Ms = Ms
  var _gammas = gammas
  var _noises = noises
  var _sfs = sfs

  def normalize_X(X: DenseMatrix[Double], X_min: DenseVector[Double], X_max: DenseVector[Double]): DenseMatrix[Double] = {
    val tmp = X(*, ::) - X_min
    return tmp(*, ::) / (X_max - X_min)
  }

  def denormalize_X(X: DenseMatrix[Double], X_min: DenseVector[Double], X_max: DenseVector[Double]): DenseMatrix[Double] = {
    val tmp: DenseMatrix[Double] = X(*, ::) :* (X_max - X_min)
    return (tmp(*, ::) + X_min).toDenseMatrix
  }

  def normalize_Y(Y: DenseMatrix[Double], Y_means: DenseVector[Double]): DenseMatrix[Double] = {
    // Only change mean values to get better prior
    //Need to accout for it during predictions
    return Y(*, ::) - Y_means
  }

  def denormalize_Y(Y: DenseMatrix[Double], Y_means: DenseVector[Double]): DenseMatrix[Double] = {
    return Y(*, ::) + Y_means
  }

  def select_features(Y: DenseMatrix[Double], Y_min: DenseVector[Double], Y_max: DenseVector[Double],
                      k_features: Int, drop_redundant: Boolean = true,
                      rtol: Double = 0.01, atol: Double = 0.00001): IndexedSeq[Int] = {
    if (k_features <= 0) { throw new IllegalArgumentException("k_features must be positive")}
    val k = if (k_features > Y.cols) Y.cols else k_features

    // LPLogger.debug("select_features: k", k)
    // LPLogger.debug("select_features: Y_min", Y_min)
    // LPLogger.debug("select_features: Y_max", Y_max)
    val scores = DenseVector.zeros[Double](k)
    for (i <- 0 to k - 1) {
      scores(i) = sqrt(meanAndVariance(Y(::, i)).variance) / (Y_max(i) - Y_min(i))
    }
    var inds = argsort(scores).reverse
    // LPLogger.debug("scores", scores)
    // LPLogger.debug("inds", inds)
    // LPLogger.debug("drop", drop_redundant)
    if (drop_redundant) {
      breakable {
        //LPLogger.debug("inds length", inds.length - 1)
        for (i <- 0 to inds.length - 1) {
          val ind = inds(i)
          // LPLogger.debug("ind", ind, "var", scores(i))
          if (~=(scores(ind), 0, atol) || scores(ind) / scores(inds(0)) <  rtol) {
            inds = inds.slice(0, i)
            // LPLogger.debug(inds)
            break
          }
          //          if (~=(scores(ind), 0.0, 0.00001)) {
          //            inds = inds.slice(0, i)
          //            LPLogger.debug(inds)
          //            break
          //          }
        }
      }
    }
    inds = inds.slice(0, min(inds.length, k))
    inds = inds.sorted

    //    LPLogger.debug("new inds", inds)
    //    LPLogger.debug("new scores", scores(inds).toDenseVector)

    return inds
  }

  def fit_gpr(estimate: Boolean = false, use_default_params: Boolean = false) = {
    val X: DenseMatrix[Double] = this._X_normed
    val Y: DenseMatrix[Double] = this._Y_normed(::, this._selected_features).toDenseMatrix
    val X_min = min(X(::, *)).t
    val X_max = max(X(::, *)).t
    val Y_min = this._Y_min(this._selected_features).toDenseVector
    val Y_max = this._Y_max(this._selected_features).toDenseVector

    val n = X.rows
    val d = X.cols
    val m = Y.cols

    // LPLogger.debug("X.rows, Y.rows: ", X.rows, Y.rows)
    assert(X.rows == Y.rows)

    if(estimate) {
      assert(this._sfs == None && this._gammas == None && this._noises == None)
    } else if (!use_default_params) {
      assert(this._sfs != None && this._gammas != None && this._noises != None)
      assert(this._gammas.get.length == this._noises.get.length && this._noises.get.length == this._sfs.get.length)
    }

//    LPLogger.debug("estimate", estimate)
//    LPLogger.debug("use default parameters", use_default_params)

    val sf_0: DenseVector[Double] = 0.01 * (Y_max - Y_min)
    val l_0: DenseVector[Double] = 0.1 * breeze.linalg.norm(X_max - X_min) * DenseVector.ones[Double](m)
    val noise_0: DenseVector[Double] = sf_0.copy

//    LPLogger.debug("Default parameters")
//    LPLogger.debug("sf_0", sf_0)
//    LPLogger.debug("l_0", l_0)
//    LPLogger.debug("gamma_0", 0.5 / (l_0 :* l_0))
//    LPLogger.debug("noise_0", noise_0)

    val sfs: DenseVector[Double] = this._sfs.getOrElse(sf_0)
    //    var gammas = sfs
    val gammas: DenseVector[Double] = this._gammas.getOrElse(0.5 / (l_0 :* l_0))
    val noises: DenseVector[Double] = this._noises.getOrElse(noise_0)

//    LPLogger.debug("sfs", sfs)
//    LPLogger.debug("ls", breeze.numerics.sqrt(0.5 / gammas))
//    LPLogger.debug("gammas", gammas)
//    LPLogger.debug("noises", noises)

    val Ms = Array.ofDim[DenseMatrix[Double]](Y.cols)

    for (i <- 0 to Y.cols - 1) {
      var covFunc = CovSEiso()
      var covFuncParams = DenseVector(
        log(sfs(i)),
        log(sqrt( 0.5 / gammas(i)))
      )
      var noiseLogStdDev: Double = log(noises(i))

      val model = if (estimate) {
        //        LPLogger.debug("estimate")
        //        LPLogger.debug("estimate: X", X.toDenseVector)
        //        LPLogger.debug("estimate: Y", (Y(::, i)).toDenseVector)
        var model_est = gpr(X, (Y(::, i)).toDenseVector, covFunc, covFuncParams, noiseLogStdDev)
        //        LPLogger.debug("estimate: covFuncParams", model_est.covFuncParams)
        //        LPLogger.debug("estimate: noiseLogStdDev", model_est.noiseLogStdDev)
        //        LPLogger.debug("estimate: sf", exp(model_est.covFuncParams(0)))
        model_est
      } else {
        //        LPLogger.debug("no estimate")
        GprModel(X, (Y(::, i)).toDenseVector, covFunc, covFuncParams, noiseLogStdDev)
      }

      val v = DenseVector.zeros[Double](1)
      v(0) = 10
      //      LPLogger.debug("v", v)

      sfs(i) = exp(model.covFuncParams(0))
      gammas(i) = 0.5 / pow(exp(model.covFuncParams(1)), 2.0)
      noises(i) = exp(model.noiseLogStdDev)

      Ms(i) = {
        val kXX = model.calcKXX()
        //        LPLogger.debug("kXX", kXX.toDenseVector)
        //        try{
        val kXXInv = model.calcKXXInv(kXX)
        kXXInv
        //        } catch {
        //          case _ : breeze.linalg.MatrixSingularException => {
        //            LPLogger.debug("Singular kXX matrix!")
        //            diag(DenseVector.ones[Double](n)*Double.PositiveInfinity)
        //          }
        //        }
      }
    }

    //    LPLogger.debug("new parameters for col 0")
    //    LPLogger.debug("new Ms", Ms(0).toDenseVector)
    //    LPLogger.debug("new sfs", sfs(0))
    //    LPLogger.debug("new gammas", gammas(0))
    //    LPLogger.debug("new ls", sqrt(0.5 / gammas(0)))
    //    LPLogger.debug("new noises", noises(0))

    this._Ms = Option(Ms)
    this._sfs = Option(sfs)
    this._gammas = Option(gammas)
    this._noises = Option(noises)
  }

  def predict_gpr_scalar(X: DenseMatrix[Double], component: Int): (DenseVector[Double],DenseVector[Double])  = {
    // WRONG INDEXING HERE, component for M and data is not the same, need to fix
    if (!this._selected_features.contains(component)) {
      throw new IllegalArgumentException("Feature %d is ignored".format(component))
    }
    val X_train_normed: DenseMatrix[Double] = this._X_normed
    val y_train_normed: DenseVector[Double] = this._Y_normed(::, component)
    val M: DenseMatrix[Double] = this._Ms.get(component)
    val gamma: Double = this._gammas.get(component)
    val sf: Double = this._sfs.get(component)

    val X_normed: DenseMatrix[Double] = if (this.normalize_x) {
      normalize_X(X, this._X_min, this._X_max)
    } else {
      X
    }

    val n = X.rows
    val y_test = DenseVector.zeros[Double](n)
    val std_test = DenseVector.zeros[Double](n)
    for (i <- 0 to n - 1) {
      val k = get_k(X_train_normed, X_normed(i, ::).t, gamma=gamma, sf=sf)
      y_test(i) = get_u(M=M, y=y_train_normed, k=k)
      std_test(i) = get_s(M=M, k=k, sf=sf)
    }
    return if (normalize_y) {
      (denormalize_Y(DenseMatrix(y_test).t, this._Y_means(component to component)).toDenseVector, std_test)
    } else {
      (y_test, std_test)
    }
  }

  def eucl_sqr(X: DenseMatrix[Double], x: DenseVector[Double]): DenseVector[Double] = {
    var dif = X(*, ::) - x
    dif = dif :* dif
    var d2 = sum(dif(*, ::))
    return d2
  }

  def rbf(d2: DenseVector[Double], gamma: Double, sf: Double): DenseVector[Double] = {
    return scala.math.pow(sf, 2.0)*exp(-gamma*d2)
  }

  def get_k(d2: DenseVector[Double], gamma: Double, sf: Double): DenseVector[Double] = {
    return rbf(d2, gamma, sf)
  }

  def get_k(X: DenseMatrix[Double], x: DenseVector[Double], gamma: Double, sf: Double): DenseVector[Double] ={
    return get_k(eucl_sqr(X, x), gamma, sf)
  }

  def get_grad_k(k: DenseVector[Double], gamma: Double, X: DenseMatrix[Double], x: DenseVector[Double]) : DenseMatrix[Double] = {
    var tmp = X(*, ::) - x
    tmp = tmp(::, *) :* k
    return 2*gamma*tmp
  }

  def get_H_k(ki: Double, gamma: Double, Xi: DenseVector[Double], x: DenseVector[Double]): DenseMatrix[Double] = {
    return 2*gamma*ki*(2*gamma*(Xi - x)*(Xi-x).t - DenseMatrix.eye[Double](x.length))
  }

  def get_u(M: DenseMatrix[Double], y: DenseVector[Double], k: DenseVector[Double]) : Double = {
    return (k.t * M) * y
  }

  def get_grad_u(M: DenseMatrix[Double], y: DenseVector[Double], grad_k: DenseMatrix[Double]): DenseVector[Double] = {
    return grad_k.t * (M * y)
  }

  def get_H_u(M: DenseMatrix[Double], y: DenseVector[Double], H_ks: Array[DenseMatrix[Double]]): DenseMatrix[Double] = {
    val n = M.rows
    val d = H_ks(0).rows
    assert(H_ks(0).rows == H_ks(0).cols)
    val c = M * y
    var H_u = DenseMatrix.zeros[Double](d,d)
    for (i <- 0 to n - 1) {
      H_u += c(i)*H_ks(i)
    }
    return H_u
  }

  def get_s(M: DenseMatrix[Double], k: DenseVector[Double], sf: Double): Double = {
    return sqrt(pow(sf, 2.0) - k.t * (M * k))
  }

  def get_grad_s(M: DenseMatrix[Double], s: Double, k: DenseVector[Double], grad_k: DenseMatrix[Double]): DenseVector[Double] = {
    var tmp = - grad_k.t * (M * k) / 2.0 / s
    //M is symmetric
    tmp *= 2.0
    return tmp

    //    return - grad_k.t * (M * k) - ((k.t * M) * grad_k).t
  }

  def get_H_s(M: DenseMatrix[Double], k: DenseVector[Double],
              s: Double, grad_s: DenseVector[Double],
              grad_k: DenseMatrix[Double], H_ks: Array[DenseMatrix[Double]]): DenseMatrix[Double] = {
    val n = M.rows
    val d = H_ks(0).rows
    assert(H_ks(0).rows == H_ks(0).cols)
    var H_s2 = DenseMatrix.zeros[Double](d,d)
    //M is symmetric => 2*
    // Grad term
    H_s2 += - 2.0 * (M * grad_k).t * grad_k
    // Hessian term
    val c = - 2.0 * M * k
    for (i <- 0 to n - 1) {
      H_s2 += c(i)*H_ks(i)
    }
    val H_s = (H_s2 - 2.0 * grad_s * grad_s.t) / (2.0 * s)
    return H_s
  }

  def get_H_1_s2(s: Double, grad_s: DenseVector[Double], H_s: DenseMatrix[Double]): DenseMatrix[Double] = {
    return 2.0 / scala.math.pow(s, 3.0) * (3.0 * grad_s * grad_s.t / s - H_s)
  }

  def get_H_u_1_s2(u: Double, s: Double,
                   grad_u: DenseVector[Double], grad_s: DenseVector[Double],
                   H_u: DenseMatrix[Double], H_1_s2: DenseMatrix[Double]): DenseMatrix[Double] = {
    var ret = H_u / scala.math.pow(s, 2.0)
    ret += - 2.0 / scala.math.pow(s, 3.0) * (grad_s * grad_u.t + grad_u * grad_s.t)
    ret += u * H_1_s2
    return ret
  }

  def get_H_u2_1_s2(u: Double, s: Double,
                    grad_u: DenseVector[Double], grad_s: DenseVector[Double],
                    H_1_s2: DenseMatrix[Double], H_u_1_s2: DenseMatrix[Double]): DenseMatrix[Double] = {

    var ret = 2.0 * u * H_u_1_s2
    ret += 2.0 / scala.math.pow(s, 2.0) * grad_u * grad_u.t
    ret += - scala.math.pow(u, 2.0) * H_1_s2
    return ret
  }

  def get_H_logs(s: Double, grad_s: DenseVector[Double], H_s: DenseMatrix[Double]): DenseMatrix[Double] = {
    return  (H_s - grad_s * grad_s.t / s) / s
  }

  def get_FIM(x: DenseVector[Double]): DenseMatrix[Double] = {
    val x_normed = if (normalize_x) {
      normalize_X(x.toDenseMatrix, this._X_min, this._X_max).toDenseVector
    } else x
    val X_normed = this._X_normed
    val Y_normed = this._Y_normed(::, this._selected_features).toDenseMatrix
    val m = Y_normed.cols
    val n = X_normed.rows
    val d = X_normed.cols
    val d2 = eucl_sqr(X=X_normed, x=x_normed)

    //    var FIM = DenseMatrix.zeros[Double](d,d)
    val FIMs: Array[DenseMatrix[Double]] = Array.ofDim[DenseMatrix[Double]](this._selected_features.length)

    for (i <- 0 until Y_normed.cols) {
      var FIM = DenseMatrix.zeros[Double](d,d)
      val M = this._Ms.get(i)
      val gamma = this._gammas.get(i)
      val sf = this._sfs.get(i)
      val mean = this._Y_means(i)

      //      LPLogger.debug("gamma", gamma)
      //      LPLogger.debug("sf", sf)
      //      LPLogger.debug("mean", mean)
      //      LPLogger.debug("M", M)

      val k = get_k(d2=d2, gamma=gamma, sf=sf)
      val y = Y_normed(::,i)
      //Account for possibly subtracted mean
      val u = mean + get_u(M=M, y=y, k=k)
      val s = get_s(M=M, k=k, sf=sf)
      val grad_k = get_grad_k(k=k, gamma=gamma, X=X_normed, x=x_normed)
      val grad_u = get_grad_u(M=M, y=y, grad_k=grad_k)
      val grad_s = get_grad_s(M=M, s=s, k=k, grad_k=grad_k)

      var H_ks = Array.ofDim[DenseMatrix[Double]](n)
      for (j <- 0 to n-1) {
        H_ks(j) = get_H_k(ki = k(j), gamma=gamma, Xi = X_normed(j, ::).t, x = x_normed)
      }

      val H_u = get_H_u(M=M, y=y, H_ks=H_ks)
      val H_s = get_H_s(M=M, k=k, s=s, grad_s=grad_s, grad_k=grad_k, H_ks=H_ks)
      val H_1_s2 = get_H_1_s2(s=s, grad_s=grad_s, H_s=H_s)
      val H_u_1_s2 = get_H_u_1_s2(u=u, s=s, grad_u=grad_u, grad_s=grad_s, H_u=H_u, H_1_s2=H_1_s2)
      val H_u2_1_s2 = get_H_u2_1_s2(u=u, s=s, grad_u=grad_u, grad_s=grad_s, H_1_s2=H_1_s2, H_u_1_s2=H_u_1_s2)
      val H_logs = get_H_logs(s=s, grad_s=grad_s, H_s=H_s)

      FIM += (scala.math.pow(u, 2.0) + scala.math.pow(s, 2.0))*H_1_s2
      FIM += H_u2_1_s2
      FIM += - 2.0 * u * H_u_1_s2
      FIM += 2.0 * H_logs
      FIM *= 0.5
      FIMs(i) = FIM
      //      LPLogger.debug("Feature %d: FIM: %d x %d:\n".format(this._selected_features(i), FIM.rows, FIM.cols), FIM)
    }
    var FIM_total = DenseMatrix.zeros[Double](d,d)
    for (i <- 0 to FIMs.length - 1) {
      FIM_total += FIMs(i)
    }
    FIM_total
  }

  /**
    * Returns error bound from CRLB (root of trace of inverse Fisher Information Matrix)
    * @param x - vector of coordinates
    * @param pinv_cond - cut singular values Si (i=1, 2, ...) smaller (in modulus) than S0*pinv_cond.
    *                  If topolgy is corridor-like, but in 2D, then should cut small singular values in order
    *                  to avoid infinite CRLB.
    * @return
    */
  def get_CRLB(x: DenseVector[Double], pinv_cond: Double = 1e-15): Double = {
    //    return try {
    //      val svd = breeze.linalg.svd()
    //      sqrt(breeze.linalg.trace(breeze.linalg.inv(get_FIM(x=x))))
    //    } catch {
    //      case _ : breeze.linalg.MatrixSingularException => {
    //        Double.PositiveInfinity
    //      }
    //    }
    return {
      val FIM = get_FIM(x=x)
      val breeze.linalg.svd.SVD(u, s, vt) = svd(FIM)
      var k: Int = 1
      for (i <- 1 to s.length - 1) {
        if (scala.math.abs(s(i)) >= scala.math.abs(s(0)*pinv_cond)) k += 1
      }
      val Uk = u(::, 0 to k - 1)
      val sk = s(0 to k - 1)
      val Vtk = vt(0 to k - 1, ::)
      val FIMinv = Vtk.t * diag(1.0 / sk) * Uk.t
      sqrt(breeze.linalg.trace(FIMinv))
    }
  }

  /**
    * @param X - matrix of coordinates
    * @return
    */
  def get_CRLB(X: DenseMatrix[Double], pinv_cond: Double): DenseVector[Double] = {
    val crlbs = DenseVector.zeros[Double](X.rows)
    // LPLogger.debug("lsolea01: ACCES",X.rows)
    for (i <- 0 until X.rows) {
      crlbs(i) = get_CRLB(X(i, ::).t, pinv_cond)
    }

    //  lsolea01 x write to file
    //    val file_io = new PrintWriter("X_floor_2.txt")
    //    for (i <- 0 until X.rows) {
    //      file_io.LPLogger.debug(X(i,::))
    //    }
    //    file_io.close()
    crlbs
  }

  def ~=(x: Double, y: Double, precision: Double) = {
    if ((x - y).abs < precision) true else false
  }

  def get_column_means(A: DenseMatrix[Double]): DenseVector[Double] = {
    val means = DenseVector.zeros[Double](A.cols)
    for (i <- 0 to A.cols - 1) {
      val mv = meanAndVariance(A(::, i))
      means(i) = mv.mean
    }
    return means
  }
}
