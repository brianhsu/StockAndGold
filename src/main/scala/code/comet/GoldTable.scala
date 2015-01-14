package code.comet

import code.lib._
import code.lib.DateToCalendar._
import code.model._
import java.text.SimpleDateFormat
import net.liftweb.actor._
import net.liftweb.common._
import net.liftweb.http.CometActor
import net.liftweb.http.CometListener
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.ListenerManager
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.util._
import net.liftweb.util.Helpers._
import org.bone.soplurk.api._
import org.bone.soplurk.constant.Qualifier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._
import java.util.Calendar

case object UpdateTable

object GoldTable extends LiftActor with ListenerManager {

  def createUpdate = UpdateTable

  def notifyBuy(newPrice: Gold) = {

    val userList = User.findAll("isBuyGoldNotified", false)
                       .filterNot(_.buyGoldAt.get.isEmpty)
                       .filter(newPrice.bankSellPrice.get <= _.buyGoldAt.get.get)

    userList.foreach { user =>

      val message = 
        s"黃金存摺銀賣買出 ${newPrice.bankSellPrice} / 每克，" +
        s"已達目標買價 ${user.buyGoldAt.get.get}"

      if (user.nickname.get == "brianhsu") {
        PrivateMessanger.sendMessage(user, message)
        user.isBuyGoldNotified(true).saveTheRecord()
      } else {
        user.postPlurk(message).foreach { x =>
          user.isBuyGoldNotified(true).saveTheRecord()
        }
      }
    }
  }

  def notifyTarget(): Unit = {

    Future {

      GoldInHand.findAll.foreach { goldInHand => 
        goldInHand.notifySell()
        updateListeners() 
      }

      Gold.find("bankName", "TaiwanBank").foreach { newPrice => 
        notifyBuy(newPrice)
        updateListeners()
      }

    }.onComplete { _ => 
      Schedule(() => notifyTarget(), 30.seconds) 
    }
  }

  def updateGoldPriceInDB(): Unit = {
    Future {

      val calendar = Calendar.getInstance
      val hour = calendar.get(Calendar.HOUR_OF_DAY)

      if (hour >= 8 && hour <= 21) {
        Gold.updateNewPrice()
      }

      updateListeners()
    }.onComplete { _ =>
      Schedule(() => updateGoldPriceInDB(), 3.minutes)
    }
  }

  override def lowPriority = {
    case UpdateTable => updateListeners()
  }

  def init() {
    updateGoldPriceInDB()
    notifyTarget()
  }
}


class GoldTable extends CometActor with CometListener {

  def registerWith = GoldTable

  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  private val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")

  def onDelete(rowID: String, value: String): JsCmd = {
    GoldInHand.delete("_id", rowID)
    this ! UpdateTable
  }

  def setBuyGoldTarget(value: String): JsCmd = {
    for {
      buyTarget <- asInt(value)
      currentUser <- User.currentUser.get
    } {
      currentUser.buyGoldAt(buyTarget)
                 .isBuyGoldNotified(false)
                 .saveTheRecord()
                 .foreach(s => S.notice(s"已設定新的黃金買入目票為 $value 元"))
    }

    this ! UpdateTable
  }

  def render = {

    val currentPrice = Gold.find("bankName", "TaiwanBank")
    def formatTimestamp(gold: Gold) = dateTimeFormatter.format(gold.priceUpdateAt.get.getTime)
    val buyGoldTarget = User.currentUser.get.map(_.buyGoldAt.toString).getOrElse("")
    val goldInHands = User.currentUser.map { user =>
      GoldInHand.findAll("userID", user.id.get.toString)
                .sortWith(_.buyDate.get.getTime.getTime < _.buyDate.get.getTime.getTime)
    }

    "#buyTarget" #> SHtml.ajaxText(buyGoldTarget, false, setBuyGoldTarget _) &
    ".bankBuy *" #> currentPrice.map(_.bankBuyPrice.toString).getOrElse(" - ") &
    ".bankSell *" #> currentPrice.map(_.bankSellPrice.toString).getOrElse(" - ") &
    ".priceUpdateAt *" #> currentPrice.map(formatTimestamp).getOrElse(" - ") &
    ".row" #> goldInHands.getOrElse(Nil).map { gold =>

      val totalPrice = (gold.buyPrice.get * gold.quantity.get)
      val newTotalPrice = currentPrice.map(_.bankBuyPrice.get * gold.quantity.get)
      val difference = newTotalPrice.map(_ - totalPrice)
      def formatNotifiedTime(calendar: java.util.Calendar) = {
        val dateTimeString = dateTimeFormatter.format(calendar.getTime)
        <div>V</div>
        <div>{dateTimeString}</div>
      }

      val targetLooseUnitPrice = ((totalPrice - gold.targetLoose.get) / gold.quantity.get).abs
      val targetEarningUnitPrice = (totalPrice + gold.targetEarning.get) / gold.quantity.get

      ".row [id]" #> s"gold-row-${gold.id}" &
      ".buyDate *" #> dateFormatter.format(gold.buyDate.get.getTime) &
      ".quantity *" #> gold.quantity &
      ".unitPrice *" #> gold.buyPrice &
      ".totalPrice *" #> totalPrice &
      ".newTotalPrice *" #> newTotalPrice.map(_.toString).getOrElse(" - ") &
      ".estEarningLoose *" #> difference.map(x => PriceFormatter(x)).getOrElse(<span>-</span>) &
      ".targetLoose *" #> gold.targetLoose &
      ".targetEarning *" #> gold.targetEarning &
      ".targetLooseUnit *" #> s"$targetLooseUnitPrice / 克" &
      ".targetEarningUnit *" #> s"$targetEarningUnitPrice / 克" &
      ".isNotified *" #> gold.notifiedAt.get.map(formatNotifiedTime) &
      ".delete [onclick]" #> SHtml.onEventIf("確定要刪除嗎？", onDelete(gold.id.toString, _))
    }
  }

  override def lowPriority = {
    case UpdateTable => reRender(true)
  }

}
