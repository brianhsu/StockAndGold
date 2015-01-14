package code.comet

import code.lib._
import code.model._
import java.text.SimpleDateFormat
import net.liftweb.actor._
import net.liftweb.http.CometActor
import net.liftweb.http.CometListener
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.ListenerManager
import net.liftweb.http.SHtml
import net.liftweb.util.{Currency => _, _}
import net.liftweb.util.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._
import code.lib.DateToCalendar._
import java.util.Calendar

class CurrencyBuyTable extends CometActor with CometListener{

  def registerWith = CurrencyTable

  private val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  private def currencyToBuy = User.currentUser.map { user =>
    CurrencyToBuy.findAll("userID", user.id.get.toString)
  }

  def onDelete(rowID: String, value: String): JsCmd = {
    CurrencyToBuy.delete("_id", rowID)
    this ! UpdateTable
  }

  def render = {

    ".row" #> currencyToBuy.getOrElse(Nil).map { currency =>

      def formatNotifiedTime(calendar: java.util.Calendar) = {
        val dateTimeString = dateTimeFormatter.format(calendar.getTime)
        <div>V</div>
        <div>{dateTimeString}</div>
      }

      val currencyInfo = Currency.find("code", currency.code.toString)
      val currentPrice = currencyInfo.map(_.bankSellPrice.get)
      val priceUpdateAt = currencyInfo.map(_.priceUpdateAt.get)
      val currencyName = Currency.currencyCodeToName
                                 .get(currency.code.toString)
                                 .getOrElse("Unknown")
 
      ".row [id]" #> s"currencyToBuy-row-${currency.id}" &
      ".name *" #> currencyName &
      ".unitPrice *" #> currency.unitPrice &
      ".isNotified *" #> currency.notifiedAt.get.map(formatNotifiedTime) &
      ".currentPrice *" #> currentPrice.map(_.toString).getOrElse("-") &
      ".priceUpdateAt *" #> priceUpdateAt.map(x => dateTimeFormatter.format(x.getTime)).getOrElse("-") &
      ".delete [onclick]" #> SHtml.onEventIf("確定要刪除嗎？", onDelete(currency.id.toString, _))
    }
  }

  override def lowPriority = {
    case UpdateTable => reRender(true)
  }
}


