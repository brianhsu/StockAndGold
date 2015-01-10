package code.snippet

import code.lib.DateToCalendar._
import code.model._
import net.liftweb.common._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.util._
import net.liftweb.util.Helpers._
import code.comet._

class AddBuyStockForm {
  private var stockCode: String = _
  private var unitPrice: Box[Double] = Empty
  private val allStockCode = Stock.stockTable.map(_.code).toSet

  def checkStockCode(stockCode: String) = {
    allStockCode.contains(stockCode) match {
      case true  => None
      case false => Some("無法找到這個股票，煩請檢查")
    }
  }

  def checkPrice[T](title: String, data: Box[T]): Option[String] = {
    data match {
      case Full(x) => None
      case _ => Some(s"$title 不是正確的數字，煩請檢查")
    }
  }

  def addBuyStock() = {

    def saveToDB(): JsCmd = {
      val newRecord = for {
        currentUser <- User.currentUser.get
        unitPrice <- this.unitPrice
        newRecord <- StockToBuy.createRecord
                               .userID(currentUser.id.toString)
                               .stockID(stockCode)
                               .unitPrice(unitPrice)
                               .updateAt(now)
                               .saveTheRecord()
      } yield newRecord

      newRecord match {
        case Some(record) => S.notice("成功新增賣入目標")
        case _ => S.error("無法存檔，請稍候再試")
      }

      StockTable ! UpdateTable
    }

    val errors = List(
      checkStockCode(stockCode),
      checkPrice("目標成交價", unitPrice)
    ).flatten

    errors match {
      case Nil => saveToDB()
      case _   => errors.foreach(S.error)
    }

    StockBuyTable ! UpdateTable
    Noop

  }

  def render = {

    "#addBuyStockCode [onchange]" #> SHtml.onEvent(stockCode = _) &
    "#addBuyStockPrice" #> SHtml.ajaxText("", false, (x: String) => {unitPrice = asDouble(x); Noop}) &
    "#addBuyStockButton" #> SHtml.ajaxOnSubmit(addBuyStock _)
  }

}

