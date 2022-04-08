package cy.ac.ucy.cs.anyplace.smas.ui.chat.utils

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// TODO:PM:ATH merge w/ utlTime / utlDate
@RequiresApi(Build.VERSION_CODES.O)
class DateTimeHelper {

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

    fun isSameDay(timestr: String) : Boolean{
      val currDate = getLocalDateString()
      val date = getDateFromStr(timestr)
      Log.d("isSameDay","$currDate $date")

      if (currDate.equals(date))
        return true
      return false
    }

}