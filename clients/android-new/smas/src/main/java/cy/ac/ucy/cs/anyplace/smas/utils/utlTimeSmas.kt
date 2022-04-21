package cy.ac.ucy.cs.anyplace.smas.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
object utlTimeSmas {

  fun getLocalDateString() : String {
    var currDate =  LocalDate.now()
    var formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val formattedDate = currDate.format(formatter)
    return formattedDate
  }

  fun getLocalTimeString() : String {
    return LocalTime.now().toString().substringBeforeLast('.').substringBeforeLast(":")
  }

  fun getDateFromStr(date : String) : String{
    return date.substringBeforeLast(' ')
  }

  fun getTimeFromStr(date : String) : String{
    return date.substringAfterLast(' ').substringBeforeLast(":")
  }

  fun getTimeFromStrFull(date : String) : String{
    return date.substringAfterLast(' ')
  }

  fun isSameDay(timestr: String) : Boolean {
    val currDate = getLocalDateString()
    val date = getDateFromStr(timestr)

    if (currDate.equals(date))
      return true
    return false
  }

}