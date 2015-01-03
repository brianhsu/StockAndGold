package code.lib

import java.util.Calendar
import java.util.Date

object DateToCalendar {
  implicit def apply(date: Date): Calendar = {
    val calendar = Calendar.getInstance
    calendar.setTime(date)
    calendar
  }
}

