package code.comet

import code.model._
import java.text.SimpleDateFormat
import net.liftweb.actor._
import net.liftweb.common._
import net.liftweb.http.CometActor
import net.liftweb.http.CometListener
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.ListenerManager
import net.liftweb.http.SHtml
import net.liftweb.util._
import net.liftweb.util.Helpers._
import org.bone.soplurk.api._
import org.bone.soplurk.constant.Qualifier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._
import code.lib.DateToCalendar._

object StockTable extends LiftActor with ListenerManager {

  def createUpdate = UpdateTable

  override def lowPriority = {
    case UpdateTable => updateListeners()
  }

  def notifyTarget() {

    def isReachedLimit(stockInHand: StockInHand): Boolean = {
      val currentPrice = Stock.find("code", stockInHand.stockID.toString).map(_.currentPrice.get)
      val totalPrice = (stockInHand.buyPrice.get * stockInHand.quantity.get + stockInHand.buyFee.get).toInt
      val newTotalPrice = currentPrice.map(x => (stockInHand.quantity.get * x).toInt)
      val differenceBox = newTotalPrice.map(_ - totalPrice)
      val isReachedLimitBox = differenceBox.map { difference =>
        if (difference < 0) {
          difference <= stockInHand.targetLoose.get.abs * -1
        } else {
          difference >= stockInHand.targetEarning.get
        }
      }

      isReachedLimitBox.openOr(false)
    }

    val sendNotification = Future {

      val notifiedList = StockInHand.findAll("isNotified", false).filter(isReachedLimit)

      for {
        stockInHand <- notifiedList
        user        <- User.find(stockInHand.userID.get)
        newPrice    <- Stock.find("code", stockInHand.stockID.toString)
      } {

        val oldTotalPrice = stockInHand.buyPrice.get * stockInHand.quantity.get + 
                            stockInHand.buyFee.get
        val newTotalPrice = newPrice.currentPrice.get * stockInHand.quantity.get
        val difference = newTotalPrice - oldTotalPrice
        val costFee = (newTotalPrice * 0.3 / 100) + (newTotalPrice * 0.145 / 100)
        val stockName = Stock.stockCodeToName.get(stockInHand.stockID.toString)
                             .getOrElse(stockInHand.stockID)

        val message = 
          s"成本為 ${oldTotalPrice} 的 $stockName 股票，" +
          s"目前市值為 $newTotalPrice ，價差為 $difference，" +
          s"已達設定停損 / 停益點 ${stockInHand.targetLoose} / ${stockInHand.targetEarning}。" +
          s"預估賣出費用為 $costFee 元"

        val newPlurk = user.postPlurk(message)

        newPlurk match {
          case Success(plurk) => 
            stockInHand.isNotified(true).notifiedAt(now).saveTheRecord()
            updateListeners()
          case _ =>
        }

      }
    }

    sendNotification.onComplete(x => Schedule(() => notifyTarget(), 30.seconds))
  }

  def updateStockPriceInDB(): Unit = {
    Stock.updateAllPrice {
      updateListeners()
      Schedule(() => updateStockPriceInDB(), 30.seconds)
    }
  }

  def init() {
    updateStockPriceInDB()
    notifyTarget()
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
        ((price * (0.1425 / 100)).round + (price * (0.3 / 100)).round)
      }
      val priceUpdateAt = Stock.find("code", stock.stockID.toString).map(_.priceUpdateAt.get)
      def formatNotifiedTime(calendar: java.util.Calendar) = {
        val dateTimeString = dateTimeFormatter.format(calendar.getTime)
        <div>V</div>
        <div>{dateTimeString}</div>
      }

      ".row [id]" #> s"stock-row-${stock.id}" &
      ".stockName *" #> Stock.stockCodeToName.get(stock.stockID.toString).getOrElse("Unknown") &
      ".buyDate *" #> dateFormatter.format(stock.buyDate.get.getTime) &
      ".quantity *" #> stock.quantity &
      ".unitPrice *" #> stock.buyPrice &
      ".totalPrice *" #> totalPrice &
      ".currentPrice *" #> currentPrice.map(_.toString).getOrElse("-") &
      ".newTotalPrice *" #> newTotalPrice.map(_.toString).getOrElse("-") &
      ".priceUpdateAt *" #> priceUpdateAt.map(x => dateTimeFormatter.format(x.getTime)).getOrElse("-") &
      ".estEarningLoose *" #> difference.map(_.toString).getOrElse(" - ") &
      ".sellCost *" #> sellCost.map(_.toString).getOrElse("-") &
      ".targetLoose *" #> stock.targetLoose &
      ".targetEarning *" #> stock.targetEarning &
      ".isNotified *" #> stock.notifiedAt.get.map(formatNotifiedTime) &
      ".delete [onclick]" #> SHtml.onEventIf("確定要刪除嗎？", onDelete(stock.id.toString, _))
    }

  }

  override def lowPriority = {
    case UpdateTable => reRender(true)
  }
}
