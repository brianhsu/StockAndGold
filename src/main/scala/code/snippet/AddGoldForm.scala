package code.snippet

import code.comet._
import code.lib.DateToCalendar._
import code.model._

import java.text.SimpleDateFormat
import net.liftweb.common._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.util._
import scala.xml.Text

class AddGoldForm {

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
        case Some(record) => S.notice("成功新增持有黃金")
        case _ =>            S.error("無法存檔，請稍候再試")
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
    GoldTable ! UpdateTable
  }

  def updateUnitPrice: JsCmd = {
    val updateLooseUnitPrice = for {
      targetLooseValue <- targetLoose
      quantityValue <- quantity
      priceValue <- price
    } yield {
      val unitPrice = (quantityValue * priceValue - targetLooseValue) / quantityValue
      JsRaw(s"""$$('#addGoldTargetLooseUnit').val('${unitPrice.toString}')""").cmd
    }

    val updateEarningUnitPrice = for {
      targetEarningValue <- targetEarning
      quantityValue <- quantity
      priceValue <- price
    } yield {
      val unitPrice = (quantityValue * priceValue + targetEarningValue) / quantityValue
      JsRaw(s"""$$('#addGoldTargetEarningUnit').val('${unitPrice.toString}')""").cmd
    }

    updateLooseUnitPrice.openOr(Noop) &
    updateEarningUnitPrice.openOr(Noop)
  }

  def setEarningUnitPrice(value: String): JsCmd = {
    val jsCmd = for {
      quantity <- this.quantity
      earningUnitPrice <- asDouble(value)
      unitPrice <- this.price
    } yield {
      val totalPrice = (earningUnitPrice * quantity - unitPrice * quantity).toInt
      targetEarning = Full(totalPrice)
      JsRaw(s"$$('#addGoldTargetEarning').val('$totalPrice')").cmd
    }
    jsCmd.openOr(Noop)
  }


  def setLooseUnitPrice(value: String): JsCmd = {
    val jsCmd = for {
      quantity <- this.quantity
      loseUnitPrice <- asDouble(value)
      unitPrice <- this.price
    } yield {
      val totalPrice = ((unitPrice * quantity) - (loseUnitPrice * quantity)).toInt
      targetLoose = Full(totalPrice)
      JsRaw(s"$$('#addGoldTargetLoose').val('$totalPrice')").cmd
    }
    jsCmd.openOr(Noop)
  }



  def render = {
    "#addGoldDate [onchange]" #> SHtml.onEvent(dateString = _) &
    "#quantity" #> SHtml.ajaxText("", false, (x: String) => {quantity = asInt(x); updateUnitPrice}) &
    "#price" #> SHtml.ajaxText("", false, (x: String) => {price = asInt(x); updateUnitPrice}) &
    "#addGoldTargetLoose" #> SHtml.ajaxText("", false, (x: String) => {targetLoose = asInt(x); updateUnitPrice}) &
    "#addGoldTargetEarning" #> SHtml.ajaxText("", false, (x: String) => {targetEarning = asInt(x); updateUnitPrice}) &
    "#addGoldTargetLooseUnit" #> SHtml.ajaxText("", false, setLooseUnitPrice _) &
    "#addGoldTargetEarningUnit" #> SHtml.ajaxText("", false, setEarningUnitPrice _) &
    "#addGoldButton" #> SHtml.ajaxOnSubmit(addGold _)
  }
}
