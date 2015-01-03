package code.comet

import org.bone.soplurk.api._
import org.bone.soplurk.constant.Qualifier
import java.text.SimpleDateFormat
import code.model._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.SHtml

import net.liftweb.http.CometActor
import net.liftweb.http.CometListener
import net.liftweb.http.ListenerManager
import net.liftweb.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._

object GoldTable extends LiftActor with ListenerManager {

  case object UpdateTable

  def createUpdate = UpdateTable

  def notifyTarget(newPrice: Gold) = Future {

    def getDifference(goldInHand: GoldInHand) = {
      val oldTotalPrice = goldInHand.buyPrice.get * goldInHand.quantity.get
      val newTotalPrice = newPrice.bankBuyPrice.get * goldInHand.quantity.get
      newTotalPrice - oldTotalPrice
    }

    def isReachedLimit(goldInHand: GoldInHand) = {
      val difference = getDifference(goldInHand)
      if (difference < 0) {
        difference <= goldInHand.targetLoose.get.abs * -1
      } else {
        difference >= goldInHand.targetEarning.get
      }
    }

    val notifiedList = GoldInHand.findAll("isNotified", false).filter(isReachedLimit)
    val appKey = "oGjxYZZMHfPE"
    val appSecret = "DpVRPOBriTqHMZIFjxjgpuTDk55LiIlK"

    for {
      goldInHand <- notifiedList
      user <- User.find(goldInHand.userID.get)
    } {
      val plurkAPI = PlurkAPI.withAccessToken(
        appKey, appSecret, 
        user.plurkToken.get, user.plurkSecret.get
      )

      val oldTotalPrice = goldInHand.buyPrice.get * goldInHand.quantity.get
      val difference = getDifference(goldInHand)
      val newTotalPrice = newPrice.bankBuyPrice.get * goldInHand.quantity.get

      val message = 
        s"買入價為 ${goldInHand.buyPrice} 的 ${goldInHand.quantity} 克黃金，" +
        s"原價 $oldTotalPrice ，目前市值為 $newTotalPrice ，價差為 $difference，" +
        s"已達設定停損 / 停益點 ${goldInHand.targetLoose} / ${goldInHand.targetEarning}"

      val newPlurk = plurkAPI.Timeline.plurkAdd(
        message, Qualifier.Says, List(user.plurkUserID.get)
      )

      println("SendNotification:" + newPlurk)

      newPlurk match {
        case Success(plurk) => goldInHand.isNotified(true).saveTheRecord()
        case _ =>
      }
    }
  }

  def updateGoldPriceInDB() {
    Gold.updateNewPrice { newPrice =>
      updateListeners()
      notifyTarget(newPrice).foreach(_ => Schedule(() => updateGoldPriceInDB(), 1000 * 60))
    }
  }

  override def lowPriority = {
    case UpdateTable => updateListeners()
  }

  def init() {
    updateGoldPriceInDB()
  }
}

class GoldTable extends CometActor with CometListener {

  def registerWith = GoldTable

  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  private val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  private def goldInHands = User.currentUser.map { user =>
    GoldInHand.findAll("userID", user.id.get.toString)
              .sortWith(_.buyDate.get.getTime.getTime < _.buyDate.get.getTime.getTime)
  }

  def onDelete(rowID: String, value: String): JsCmd = {
    GoldInHand.delete("_id", rowID)
    this ! GoldTable.UpdateTable
  }

  def render = {

    val currentPrice = Gold.find("bankName", "TaiwanBank")

    ".bankBuy *" #> currentPrice.map(_.bankBuyPrice.toString).getOrElse(" - ") &
    ".bankSell *" #> currentPrice.map(_.bankSellPrice.toString).getOrElse(" - ") &
    ".priceUpdateAt *" #> currentPrice.map(x => dateTimeFormatter.format(x.priceUpdateAt.get.getTime)).getOrElse(" - ") &
    ".row" #> goldInHands.getOrElse(Nil).map { gold =>

      val totalPrice = (gold.buyPrice.get * gold.quantity.get)
      val newTotalPrice = currentPrice.map(_.bankBuyPrice.get * gold.quantity.get)
      val difference = newTotalPrice.map(_ - totalPrice)

      ".row [id]" #> s"gold-row-${gold.id}" &
      ".buyDate *" #> dateFormatter.format(gold.buyDate.get.getTime) &
      ".quantity *" #> gold.quantity &
      ".unitPrice *" #> gold.buyPrice &
      ".totalPrice *" #> totalPrice &
      ".newTotalPrice *" #> newTotalPrice.map(_.toString).getOrElse(" - ") &
      ".estEarningLoose *" #> difference.map(_.toString).getOrElse(" - ") &
      ".targetLoose *" #> gold.targetLoose &
      ".targetEarning *" #> gold.targetEarning &
      ".delete [onclick]" #> SHtml.onEventIf("確定要刪除嗎？", onDelete(gold.id.toString, _))
    }
  }

  override def lowPriority = {
    case GoldTable.UpdateTable => reRender(true)
  }

}

