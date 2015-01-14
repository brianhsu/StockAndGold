package code.comet

import code.lib._
import code.model._
import java.text.SimpleDateFormat
import net.liftweb.actor._
import net.liftweb.http.CometActor
import net.liftweb.http.CometListener
import net.liftweb.http.js.JsCmd
import net.liftweb.http.ListenerManager
import net.liftweb.http.SHtml
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._
import code.lib.DateToCalendar._
import java.util.Calendar


class StockBuyTable extends CometActor with CometListener{

  def registerWith = StockTable

  private val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  private def stockToBuy = User.currentUser.map { user =>
    StockToBuy.findAll("userID", user.id.get.toString).sortWith(_.stockID.get < _.stockID.get)
  }

  def onDelete(rowID: String, value: String): JsCmd = {
    StockToBuy.delete("_id", rowID)
    this ! UpdateTable
  }


  def render = {

    ".row" #> stockToBuy.getOrElse(Nil).map { stock =>

      def formatNotifiedTime(calendar: java.util.Calendar) = {
        val dateTimeString = dateTimeFormatter.format(calendar.getTime)
        <div>V</div>
        <div>{dateTimeString}</div>
      }

      val stockInfo = Stock.find("code", stock.stockID.toString)
      val currentPrice = stockInfo.map(_.currentPrice.get)
      val priceUpdateAt = stockInfo.map(_.priceUpdateAt.get)

      ".row [id]" #> s"stockToBuy-row-${stock.id}" &
      ".stockName *" #> Stock.stockCodeToName.get(stock.stockID.toString).getOrElse("Unknown") &
      ".unitPrice *" #> stock.unitPrice &
      ".isNotified *" #> stock.notifiedAt.get.map(formatNotifiedTime) &
      ".currentPrice *" #> currentPrice.map(_.toString).getOrElse("-") &
      ".priceUpdateAt *" #> priceUpdateAt.map(x => dateTimeFormatter.format(x.getTime)).getOrElse("-") &
      ".delete [onclick]" #> SHtml.onEventIf("確定要刪除嗎？", onDelete(stock.id.toString, _))
    }
  }

  override def lowPriority = {
    case UpdateTable => reRender(true)
  }
}

