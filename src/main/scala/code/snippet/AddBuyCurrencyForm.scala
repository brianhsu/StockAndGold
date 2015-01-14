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


class AddBuyCurrencyForm {
  private var currencyCode: String = _
  private var unitPrice: Box[Double] = Empty
  private val allCurrencyCode = code.model.Currency.currencyList.map(_.code).toSet

  def checkCurrencyCode(currencyCode: String) = {
    allCurrencyCode.contains(currencyCode) match {
      case true  => None
      case false => Some("無法找到這個幣別，煩請檢查")
    }
  }

  def checkPrice[T](title: String, data: Box[T]): Option[String] = {
    data match {
      case Full(x) => None
      case _ => Some(s"$title 不是正確的數字，煩請檢查")
    }
  }

  def addBuyCurrency() = {

    def saveToDB(): JsCmd = {
      val newRecord = for {
        currentUser <- User.currentUser.get
        unitPrice <- this.unitPrice
        newRecord <- CurrencyToBuy.createRecord
                                  .userID(currentUser.id.toString)
                                  .code(currencyCode)
                                  .unitPrice(unitPrice)
                                  .updateAt(now)
                                  .saveTheRecord()
      } yield newRecord

      newRecord match {
        case Some(record) => S.notice("成功新增買入目標")
        case _ => S.error("無法存檔，請稍候再試")
      }
    }

    val errors = List(
      checkCurrencyCode(currencyCode),
      checkPrice("目標成交價", unitPrice)
    ).flatten

    errors match {
      case Nil => saveToDB()
      case _   => errors.foreach(S.error)
    }

    CurrencyTable ! UpdateTable
    Noop

  }

  def render = {

    "#addBuyCurrencyCode [onchange]" #> SHtml.onEvent(currencyCode = _) &
    "#addBuyCurrencyPrice" #> SHtml.ajaxText("", false, (x: String) => {unitPrice = asDouble(x); Noop}) &
    "#addBuyCurrencyButton" #> SHtml.ajaxOnSubmit(addBuyCurrency _)
  }

}
