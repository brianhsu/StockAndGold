package code.comet

import code.lib._
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
import java.util.Calendar

object StockBuyTable extends LiftActor with ListenerManager {
  def createUpdate = UpdateTable

  def startNotification() {
    Future {
      notifyTarget()
    }.onComplete { _ =>
      Schedule(() => startNotification, 30.seconds)
    }
  }

  def notifyTarget() {

    def isReachedLimit(stockToBuy: StockToBuy): Boolean = {
      val stockID = stockToBuy.stockID.toString
      val currentPrice = Stock.find("code", stockID).map(_.currentPrice.get)
      currentPrice.map(_ <= stockToBuy.unitPrice.get).openOr(false)
    }

    val notifiedList = StockToBuy.findAll("isNotified", false).filter(isReachedLimit)


    for {
      stockToBuy <- notifiedList
      user       <- User.find(stockToBuy.userID.get)
      newPrice   <- Stock.find("code", stockToBuy.stockID.toString)
    } {

      val stockName = Stock.stockCodeToName.get(stockToBuy.stockID.toString)
                           .getOrElse(stockToBuy.stockID)

      val message = s"股票 ${stockToBuy.stockID} $stockName 的成交價為 ${newPrice.currentPrice}，已到達設定的買入點 ${stockToBuy.unitPrice}"

      if (user.nickname.get == "brianhsu") {
        PrivateMessanger.sendMessage(user, message)
        stockToBuy.isNotified(true).notifiedAt(now).saveTheRecord()
      } else {
        val newPlurk = user.postPlurk(message)
        user.xmppAddress.get.foreach(address => XMPPMessanger.send(address, message))
        newPlurk.foreach { plurk =>
          stockToBuy.isNotified(true).notifiedAt(now).saveTheRecord()
        }
      }
      updateListeners()
    }
  }

  override def lowPriority = {
    case UpdateTable => updateListeners()
  }

  def updateStockPriceInDB(): Unit = {
    Future {

      val calendar = Calendar.getInstance
      val hour = calendar.get(Calendar.HOUR_OF_DAY)

      if (hour >= 9 && hour <= 14) {
        Stock.updateAllPrice()
      }

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

class StockBuyTable extends CometActor with CometListener{

  def registerWith = StockBuyTable

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

object StockTable extends LiftActor with ListenerManager {

  def createUpdate = UpdateTable

  override def lowPriority = {
    case UpdateTable => updateListeners()
  }

  def startNotification() {
    Future {
      notifyTarget()
    }.onComplete { _ =>
      Schedule(() => startNotification, 30.seconds)
    }
  }

  def notifyTarget() {

    def isReachedLimit(stockInHand: StockInHand): Boolean = {

      val stockID = stockInHand.stockID.toString

      val currentPrice = Stock.find("code", stockID).map(_.currentPrice.get)
      val totalPrice = 
        (stockInHand.buyPrice.get * stockInHand.quantity.get + 
         stockInHand.buyFee.get).toInt

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

      if (user.nickname.get == "brianhsu") {
        PrivateMessanger.sendMessage(user, message)
        stockInHand.isNotified(true).notifiedAt(now).saveTheRecord()
      } else {
        val newPlurk = user.postPlurk(message)
        user.xmppAddress.get.foreach(address => XMPPMessanger.send(address, message))
        newPlurk.foreach { plurk =>
          stockInHand.isNotified(true).notifiedAt(now).saveTheRecord()
        }
      }
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

