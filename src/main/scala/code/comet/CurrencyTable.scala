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

object CurrencyTable extends LiftActor with ListenerManager {

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
    CurrencyInHand.findAll("isNotified", false).foreach { currencyInHand =>
      currencyInHand.notifySell()
      updateListeners()
    }

    CurrencyToBuy.findAll("isNotified", false).foreach { currencyToBuy =>
      currencyToBuy.notifyBuy()
      updateListeners()
    }
  }

  def updateCurrencyPrice(): Unit = {
    Future {
      val calendar = Calendar.getInstance
      val hour = calendar.get(Calendar.HOUR_OF_DAY)

      if (hour >= 8 && hour <= 17) {
        Currency.updateAllPrice()
      }
 
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

