package code.snippet

import code.lib.DateToCalendar._
import code.model._
import java.text.SimpleDateFormat
import net.liftweb.common._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.util._
import scala.xml.Text

class AddCurrencyForm {

  private var dateString: String = _
  private var currencyCode: String = _
  private var quantity: Box[Int] = Empty
  private var price: Box[Double] = Empty
  private var targetLoose: Box[Int] = Empty
  private var targetEarning: Box[Int] = Empty
  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

  private val allCurrencyCode = code.model.Currency.currencyList.map(_.code).toSet

  def checkDate(dateString: String): Option[String] = {
    Try(dateFormatter.parse(dateString)) match {
      case Success(_) => None
      case _ => Some("買入日期格式錯誤")
    }
  }

  def checkInt[T](title: String, data: Box[T]): Option[String] = {
    data match {
      case Full(x) => None
      case _ => Some(s"$title 不是正確的數字，煩請檢查")
    }
  }

  def checkCurrencyCode(currencyCode: String) = {
    allCurrencyCode.contains(currencyCode) match {
      case true  => None
      case false => Some("無法找到這個股票，煩請檢查")
    }
  }

  def addCurrency(): JsCmd = {

    def saveToDB(): JsCmd = {
      val newRecord = for {
        currentUser <- User.currentUser.get
        date <- Try(dateFormatter.parse(dateString)).toOption
        quantity <- this.quantity
        price <- this.price
        targetEarning <- this.targetEarning
        targetLoose <- this.targetLoose
        newRecord <- CurrencyInHand.createRecord
                               .userID(currentUser.id.toString)
                               .code(currencyCode)
                               .buyDate(date)
                               .buyPrice(price)
                               .quantity(quantity)
                               .targetEarning(targetEarning)
                               .targetLoose(targetLoose)
                               .updateAt(now)
                               .saveTheRecord()
      } yield newRecord

      newRecord match {
        case Some(record) => S.notice("成功新增外幣")
        case _ => S.error("無法存檔，請稍候再試")
      }

      //StockTable ! UpdateTable
    }

    val errors = List(
      checkCurrencyCode(currencyCode),
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

  def updateUnitPrice: JsCmd = {
    val updateLooseUnitPrice = for {
      targetLooseValue <- targetLoose
      quantityValue <- quantity
      priceValue <- price
    } yield {
      val unitPrice = (quantityValue * priceValue - targetLooseValue) / quantityValue
      JqSetHtml("addCurrencyTargetLooseUnit", Text(unitPrice.toString))
    }

    val updateEarningUnitPrice = for {
      targetEarningValue <- targetEarning
      quantityValue <- quantity
      priceValue <- price
    } yield {
      val unitPrice = (quantityValue * priceValue + targetEarningValue) / quantityValue
      JqSetHtml("addCurrencyTargetEarningUnit", Text(unitPrice.toString))
    }

    updateLooseUnitPrice.openOr(Noop) &
    updateEarningUnitPrice.openOr(Noop)
  }

  def render = {

    ".currencyListItem" #> code.model.Currency.currencyList.map { currencyInfo =>
      ".currencyListItem *" #> s"[${currencyInfo.code}] ${currencyInfo.name}" &
      ".currencyListItem [value]" #> currencyInfo.code
    } &
    "#addCurrencyCode [onchange]" #> SHtml.onEvent(currencyCode = _) &
    "#addCurrencyDate [onchange]" #> SHtml.onEvent(dateString = _) &
    "#addCurrencyQuantity" #> SHtml.ajaxText("", false, (x: String) => {quantity = asInt(x); updateUnitPrice}) &
    "#addCurrencyPrice" #> SHtml.ajaxText("", false, (x: String) => {price = asDouble(x); updateUnitPrice}) &
    "#addCurrencyTargetLoose" #> SHtml.ajaxText("", false, (x: String) => {targetLoose = asInt(x); updateUnitPrice}) &
    "#addCurrencyTargetEarning" #> SHtml.ajaxText("", false, (x: String) => {targetEarning = asInt(x); updateUnitPrice}) &
    "#addCurrencyButton" #> SHtml.ajaxOnSubmit(addCurrency _)
  }
}
