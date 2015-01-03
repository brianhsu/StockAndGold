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

class AddStockForm {

  import DateToCalendar._

  private var dateString: String = _
  private var stockCode: String = _
  private var quantity: Box[Int] = Empty
  private var price: Box[Double] = Empty
  private var buyFee: Box[Int] = Empty
  private var targetLoose: Box[Int] = Empty
  private var targetEarning: Box[Int] = Empty
  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

  private val allStockCode = Stock.stockTable.map(_.code).toSet

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

  def checkStockCode(stockCode: String) = {
    allStockCode.contains(stockCode) match {
      case true  => None
      case false => Some("無法找到這個股票，煩請檢查")
    }
  }

  def addStock(): JsCmd = {

    def saveToDB(): JsCmd = {
      val newRecord = for {
        currentUser <- User.currentUser.get
        date <- Try(dateFormatter.parse(dateString)).toOption
        quantity <- this.quantity
        price <- this.price
        targetEarning <- this.targetEarning
        targetLoose <- this.targetLoose
        newRecord <- StockInHand.createRecord
                               .userID(currentUser.id.toString)
                               .stockID(stockCode)
                               .buyDate(date)
                               .buyFee(buyFee)
                               .buyPrice(price)
                               .quantity(quantity)
                               .targetEarning(targetEarning)
                               .targetLoose(targetLoose)
                               .updateAt(now)
                               .saveTheRecord()
      } yield newRecord

      import code.comet._
      newRecord match {
        case Some(record) => 
          S.notice("成功新增股票")
          StockTable ! UpdateTable
        case _ => 
          S.error("無法存檔，請稍候再試")
      }
    }

    val errors = List(
      checkStockCode(stockCode),
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

    ".stockListItem" #> Stock.stockTable.map { stockInfo =>
      ".stockListItem *" #> stockInfo.name &
      ".stockListItem [value]" #> stockInfo.code
    } &
    "#addStockCode [onchange]" #> SHtml.onEvent(stockCode = _) &
    "#addStockDate [onchange]" #> SHtml.onEvent(dateString = _) &
    "#addStockQuantity" #> SHtml.ajaxText("", false, (x: String) => {quantity = asInt(x); Noop}) &
    "#addStockPrice" #> SHtml.ajaxText("", false, (x: String) => {price = asDouble(x); Noop}) &
    "#addStockFee" #> SHtml.ajaxText("", false, (x: String) => {buyFee = asInt(x); Noop}) &
    "#addStockTargetLoose" #> SHtml.ajaxText("", false, (x: String) => {targetLoose = asInt(x); Noop}) &
    "#addStockTargetEarning" #> SHtml.ajaxText("", false, (x: String) => {targetEarning = asInt(x); Noop}) &
    "#addStockButton" #> SHtml.ajaxOnSubmit(addStock _)
  }
}
