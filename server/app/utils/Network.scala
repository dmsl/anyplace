package utils

import java.io.{BufferedReader, InputStreamReader}
import java.net.{HttpURLConnection, URL}

object Network {

  /**
   * GET request
   *
   * @param url
   * @return
   */
  def GET(url: String) = {
    val obj = new URL(url)
    val con = obj.openConnection().asInstanceOf[HttpURLConnection]
    con.setRequestMethod("GET")
    // val responseCode = con.getResponseCode
    val in = new BufferedReader(new InputStreamReader(con.getInputStream))
    val response = new StringBuffer()
    response.append(Iterator.continually(in.readLine()).takeWhile(_ != null).mkString)
    in.close()
    response.toString
  }

}
