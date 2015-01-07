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
import net.liftweb.util.{Currency => _, _}
import net.liftweb.util.Helpers._
import org.bone.soplurk.api._
import org.bone.soplurk.constant.Qualifier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._
import code.lib.DateToCalendar._

object CurrencyTable extends LiftActor with ListenerManager {

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

    def isReachedLimit(currencyInHand: CurrencyInHand): Boolean = {

      val currencyCode = currencyInHand.code.toString
      val bankBuyPrice = Currency.find("code", currencyCode).map(_.bankBuyPrice.get)
      val totalPrice = (currencyInHand.buyPrice.get * currencyInHand.quantity.get).toInt
      val newTotalPrice = bankBuyPrice.map(x => (currencyInHand.quantity.get * x).toInt)
      val differenceBox = newTotalPrice.map(_ - totalPrice)
      val isReachedLimitBox = differenceBox.map { difference =>
        if (difference < 0) {
          difference <= currencyInHand.targetLoose.get.abs * -1
        } else {
          difference >= currencyInHand.targetEarning.get
        }
      }

      isReachedLimitBox.openOr(false)
    }

    val notifiedList = CurrencyInHand.findAll("isNotified", false).filter(isReachedLimit)

    for {
      currencyInHand  <- notifiedList
      user            <- User.find(currencyInHand.userID.get)
      newPrice        <- Currency.find("code", currencyInHand.code.toString)
    } {

      val oldTotalPrice = currencyInHand.buyPrice.get * currencyInHand.quantity.get
      val newTotalPrice = newPrice.bankBuyPrice.get * currencyInHand.quantity.get
      val difference = newTotalPrice - oldTotalPrice
      val currencyName = Currency.currencyCodeToName.get(currencyInHand.code.toString)
                           .getOrElse(currencyInHand.code)

      val message = 
        s"成本為 ${oldTotalPrice} 的 $currencyName，" +
        s"目前市值為 $newTotalPrice ，價差為 $difference，" +
        s"已達設定停損 / 停益點 ${currencyInHand.targetLoose} / ${currencyInHand.targetEarning}。" 

      if (user.nickname.get == "brianhsu") {
        PrivateMessanger.sendMessage(user, message)
      } else {

        val newPlurk = user.postPlurk(message)
        
        user.xmppAddress.get.foreach(address => XMPPMessanger.send(address, message))

        newPlurk.foreach { plurk =>
          currencyInHand.isNotified(true).notifiedAt(now).saveTheRecord()
        }
      }
      updateListeners()
    }
  }

  def updateCurrencyPrice(): Unit = {
    Future {
      Currency.updateAllPrice()
      updateListeners()
    }.onComplete { _ =>
      Schedule(() => updateCurrencyPrice(), 30.seconds)
    }
  }


  def init() {
    updateCurrencyPrice()
    startNotification()
  }

}


class CurrencyTable extends CometActor with CometListener{

  def registerWith = CurrencyTable

  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  private val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  private def currencyInHands = User.currentUser.map { user =>
    CurrencyInHand.findAll("userID", user.id.get.toString)
                  .sortWith(_.buyDate.get.getTime.getTime < _.buyDate.get.getTime.getTime)
  }

  def onDelete(rowID: String, value: String): JsCmd = {
    CurrencyInHand.delete("_id", rowID)
    this ! UpdateTable
  }

  def render = {

    ".row" #> currencyInHands.getOrElse(Nil).map { currency =>

      val totalPrice = (currency.buyPrice.get * currency.quantity.get).toInt
      val bankBuyPrice = Currency.find("code", currency.code.toString).map(_.bankBuyPrice.get)
      val newTotalPrice = bankBuyPrice.map(x => (currency.quantity.get * x).toInt)
      val difference = newTotalPrice.map(_ - totalPrice)
      val priceUpdateAt = Currency.find("code", currency.code.toString).map(_.priceUpdateAt.get)
      def formatNotifiedTime(calendar: java.util.Calendar) = {
        val dateTimeString = dateTimeFormatter.format(calendar.getTime)
        <div>V</div>
        <div>{dateTimeString}</div>
      }

      val targetLooseUnitPrice = ((totalPrice - currency.targetLoose.get) / currency.quantity.get).abs
      val targetEarningUnitPrice = (totalPrice + currency.targetEarning.get) / currency.quantity.get

      ".row [id]" #> s"currency-row-${currency.id}" &
      ".currencyName *" #> Currency.currencyCodeToName.get(currency.code.toString).getOrElse("Unknown") &
      ".buyDate *" #> dateFormatter.format(currency.buyDate.get.getTime) &
      ".quantity *" #> currency.quantity &
      ".unitPrice *" #> currency.buyPrice &
      ".totalPrice *" #> totalPrice &
      ".bankBuyPrice *" #> bankBuyPrice.map(_.toString).getOrElse("-") &
      ".newTotalPrice *" #> newTotalPrice.map(_.toString).getOrElse("-") &
      ".priceUpdateAt *" #> priceUpdateAt.map(x => dateTimeFormatter.format(x.getTime)).getOrElse("-") &
      ".estEarningLoose *" #> difference.map(x => PriceFormatter(x)).getOrElse(<span>-</span>) &
      ".targetLoose *" #> currency.targetLoose &
      ".targetLooseUnit *" #> f"$targetLooseUnitPrice%.3f" &
      ".targetEarningUnit *" #> f"$targetEarningUnitPrice%.3f" &
      ".targetEarning *" #> currency.targetEarning &
      ".isNotified *" #> currency.notifiedAt.get.map(formatNotifiedTime) &
      ".delete [onclick]" #> SHtml.onEventIf("確定要刪除嗎？", onDelete(currency.id.toString, _))
    }

  }

  override def lowPriority = {
    case UpdateTable => reRender(true)
  }
}

