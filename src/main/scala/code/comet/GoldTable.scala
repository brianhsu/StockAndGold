package code.comet

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

object GoldTable extends LiftActor with ListenerManager {

  case object UpdateTable

  def createUpdate = UpdateTable

  def updateGoldPriceInDB() {
    Gold.updateNewPrice {
      updateListeners()
      Schedule(() => updateGoldPriceInDB(), 1000 * 60)
    }
  }

  override def lowPriority = {
    case UpdateTable => updateListeners()
  }

  updateGoldPriceInDB()

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
      val difference = newTotalPrice.map(totalPrice - _)

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

