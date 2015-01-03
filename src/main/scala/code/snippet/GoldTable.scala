package code.snippet

import code.model._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.SHtml
import scala.util._
import java.text.SimpleDateFormat
import net.liftweb.http.S
import net.liftweb.common._

object DateToCalendar {
  import java.util.Calendar
  import java.util.Date
  implicit def apply(date: Date): Calendar = {
    val calendar = Calendar.getInstance
    calendar.setTime(date)
    calendar
  }
}

class GoldTable {

  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  private def goldInHands = User.currentUser.map { user =>
    GoldInHand.findAll("userID", user.id.get.toString)
              .sortWith(_.buyDate.get.getTime.getTime < _.buyDate.get.getTime.getTime)
  }

  def onDelete(rowID: String, value: String): JsCmd = {
    GoldInHand.delete("_id", rowID)
  }

  def render = {

    val currentPrice = Gold.find("bankName", "TaiwanBank")

    ".bankBuy *" #> currentPrice.map(_.bankBuyPrice.toString).getOrElse(" - ") &
    ".bankSell *" #> currentPrice.map(_.bankSellPrice.toString).getOrElse(" - ") &
    ".priceUpdateAt *" #> currentPrice.map(x => dateFormatter.format(x.priceUpdateAt.get.getTime)).getOrElse(" - ") &
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

}

class AddGoldForm {

  import DateToCalendar._

  private var dateString: String = _
  private var quantity: Box[Int] = Empty
  private var price: Box[Int] = Empty
  private var targetLoose: Box[Int] = Empty
  private var targetEarning: Box[Int] = Empty
  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

  def checkDate(dateString: String): Option[String] = {
    Try(dateFormatter.parse(dateString)) match {
      case Success(_) => None
      case _ => Some("買入日期格式錯誤")
    }
  }

  def checkInt(title: String, data: Box[Int]): Option[String] = {
    data match {
      case Full(x) => None
      case _ => Some(s"$title 不是正確的數字，煩請檢查")
    }
  }

  def addGold(): JsCmd = {

    def saveToDB(): JsCmd = {
      val newRecord = for {
        currentUser <- User.currentUser.get
        date <- Try(dateFormatter.parse(dateString)).toOption
        quantity <- this.quantity
        price <- this.price
        targetEarning <- this.targetEarning
        targetLoose <- this.targetLoose
        newRecord <- GoldInHand.createRecord
                               .userID(currentUser.id.toString)
                               .buyDate(date)
                               .buyPrice(price)
                               .quantity(quantity)
                               .targetEarning(targetEarning)
                               .targetLoose(targetLoose)
                               .updateAt(now)
                               .saveTheRecord()
      } yield newRecord

      newRecord match {
        case Some(record) => S.notice("已加入資料庫")
        case _ => S.error("無法存檔，請稍候再試")
      }
    }

    val errors = List(
      checkDate(dateString),
      checkInt("持有單位", quantity),
      checkInt("成本單價", price),
      checkInt("停損", targetLoose),
      checkInt("停益", targetEarning)
    ).flatten

    errors match {
      case Nil => saveToDB()
      case _   => errors.foreach(S.error)
    }
  }

  def render = {
    "#addGoldDate [onchange]" #> SHtml.onEvent(dateString = _) &
    "#quantity" #> SHtml.ajaxText("", false, (x: String) => {quantity = asInt(x); Noop}) &
    "#price" #> SHtml.ajaxText("", false, (x: String) => {price = asInt(x); Noop}) &
    "#targetLoose" #> SHtml.ajaxText("", false, (x: String) => {targetLoose = asInt(x); Noop}) &
    "#targetEarning" #> SHtml.ajaxText("", false, (x: String) => {targetEarning = asInt(x); Noop}) &
    "#addGoldButton" #> SHtml.ajaxOnSubmit(addGold _)
  }
}
