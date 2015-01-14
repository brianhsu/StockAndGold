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

object StockTable extends LiftActor with ListenerManager {

  def createUpdate = UpdateTable

  override def lowPriority = {
    case UpdateTable => updateListeners()
  }

  def startNotification() {
    Future {
      notifyUsers()
    }.onComplete { _ =>
      Schedule(() => startNotification, 30.seconds)
    }
  }

  def notifyUsers() {
    StockInHand.findAll("isNotified", false).foreach { stockInHand =>
      stockInHand.notifySell()
      updateListeners()
    }

    StockToBuy.findAll("isNotified", false).foreach { stockToBuy =>
      stockToBuy.notifyBuy()
      updateListeners()
    }
  }

  def updateStockPriceInDB(): Unit = {
    Future {
      Stock.updateAllPrice()
      updateListeners()
    }.onComplete { _ =>
      Schedule(() => updateStockPriceInDB(), 30.seconds)
    }
  }

  def init() {
    updateStockPriceInDB()
    startNotification()
  }

}


class StockTable extends CometActor with CometListener{

  def registerWith = StockTable


  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  private val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  private def stockInHands = User.currentUser.map { user =>
    StockInHand.findAll("userID", user.id.get.toString)
               .sortWith(_.buyDate.get.getTime.getTime < _.buyDate.get.getTime.getTime)
  }

  def onDelete(rowID: String, value: String): JsCmd = {
    StockInHand.delete("_id", rowID)
    this ! UpdateTable
  }

  def render = {

    ".row" #> stockInHands.getOrElse(Nil).map { stock =>

      val totalPrice = (stock.buyPrice.get * stock.quantity.get + stock.buyFee.get).toInt
      val currentPrice = Stock.find("code", stock.stockID.toString).map(_.currentPrice.get)
      val newTotalPrice = currentPrice.map(x => (stock.quantity.get * x).toInt)
      val difference = newTotalPrice.map(_ - totalPrice)
      val sellCost = newTotalPrice.map { price =>
        val originalFee = (price * (0.1425 / 100))
        val fee = (originalFee * 0.28).max(20)
        val tax = (price * (0.3 / 100)).round
        (fee.round + tax) * -1
      }
      val priceUpdateAt = Stock.find("code", stock.stockID.toString).map(_.priceUpdateAt.get)
      def formatNotifiedTime(calendar: java.util.Calendar) = {
        val dateTimeString = dateTimeFormatter.format(calendar.getTime)
        <div>V</div>
        <div>{dateTimeString}</div>
      }

      val targetLooseUnitPrice = ((totalPrice - stock.targetLoose.get) / stock.quantity.get).abs
      val targetEarningUnitPrice = (totalPrice + stock.targetEarning.get) / stock.quantity.get

      ".row [id]" #> s"stock-row-${stock.id}" &
      ".stockName *" #> Stock.stockCodeToName.get(stock.stockID.toString).getOrElse("Unknown") &
      ".buyDate *" #> dateFormatter.format(stock.buyDate.get.getTime) &
      ".quantity *" #> stock.quantity &
      ".unitPrice *" #> stock.buyPrice &
      ".totalPrice *" #> totalPrice &
      ".currentPrice *" #> currentPrice.map(_.toString).getOrElse("-") &
      ".newTotalPrice *" #> newTotalPrice.map(_.toString).getOrElse("-") &
      ".priceUpdateAt *" #> priceUpdateAt.map(x => dateTimeFormatter.format(x.getTime)).getOrElse("-") &
      ".estEarningLoose *" #> difference.map(x => PriceFormatter(x)).getOrElse(<span>-</span>) &
      ".sellCost *" #> sellCost.map(x => PriceFormatter(x)).getOrElse(<span>-</span>) &
      ".targetLoose *" #> stock.targetLoose &
      ".targetLooseUnit *" #> f"$targetLooseUnitPrice%.2f" &
      ".targetEarningUnit *" #> f"$targetEarningUnitPrice%.2f" &
      ".targetEarning *" #> stock.targetEarning &
      ".isNotified *" #> stock.notifiedAt.get.map(formatNotifiedTime) &
      ".delete [onclick]" #> SHtml.onEventIf("確定要刪除嗎？", onDelete(stock.id.toString, _))
    }

  }

  override def lowPriority = {
    case UpdateTable => reRender(true)
  }
}

