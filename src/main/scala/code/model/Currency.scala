package code.model

import code.lib._

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._
import net.liftweb.common._

import java.text.SimpleDateFormat
import java.util.Calendar

import scala.concurrent.ExecutionContext.Implicits.global

object Currency extends Currency with MongoMetaRecord[Currency] {

  case class CurrencyInfo(code: String, name: String)

  lazy val currencyCodeToName = currencyList.map(x => (x.code, x.name)).toMap
  lazy val currencyList = List(
    CurrencyInfo("USD", "美金"),
    CurrencyInfo("HKD", "港幣"),
    CurrencyInfo("GBP", "英鎊"),
    CurrencyInfo("AUD", "澳幣"),
    CurrencyInfo("CAD", "加拿大幣"),
    CurrencyInfo("SGD", "新加坡幣"),
    CurrencyInfo("CHF", "瑞士法郎"),
    CurrencyInfo("JPY", "日圓"),
    CurrencyInfo("ZAR", "南非幣"),
    CurrencyInfo("SEK", "瑞典幣"),
    CurrencyInfo("NZD", "紐元"),
    CurrencyInfo("THB", "泰幣"),
    CurrencyInfo("PHP", "菲國比索"),
    CurrencyInfo("IDR", "印尼幣"),
    CurrencyInfo("EUR", "歐元"),
    CurrencyInfo("KRW", "韓元"),
    CurrencyInfo("VND", "越南盾"),
    CurrencyInfo("MYR", "馬來幣"),
    CurrencyInfo("CNY", "人民幣")
  )


  def updateAllPrice() = {

    def getLastUpdateTime(csvURL: String): Calendar = {
      val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      val calendar = Calendar.getInstance
      calendar.setTime(dateFormatter.parse(csvURL.takeRight(19)))
      calendar
    }

    def getCsvURL(content: String): Option[String] = {
      val lines = content.split("\n")
      val targetLine = lines.filter(_ contains "DownloadTxt").headOption
      def extractCsvURL(line: String) = {
        val htmlNode = scala.xml.XML.loadString(line.trim)
        val csvURL = (htmlNode \\ "@href").text
        s"http://rate.bot.com.tw/${csvURL}"
      }

      targetLine.map(extractCsvURL)
    }

    val rateHTML = DataGetter("http://rate.bot.com.tw/Pages/Static/UIP003.zh-TW.htm")

    for {
      htmlData <- rateHTML
      csvURL <- getCsvURL(htmlData)
      csvData <- DataGetter(csvURL)
      lastUpdate = getLastUpdateTime(csvURL)
      record <- csvData.split("\n").drop(1)
    } {
      val columns = record.split("\\s+").toList
      val currencyCode = columns(1).trim
      val bankBuy = BigDecimal(columns(3).trim)
      val bankSell = BigDecimal(columns(13).trim)

      val newRecord = Currency.find("code", currencyCode) match {
        case Full(record) =>
          record.bankBuyPrice(bankBuy)
                .bankSellPrice(bankSell)
                .priceUpdateAt(lastUpdate)
                .saveTheRecord()

        case Empty =>
          Currency.createRecord
                  .code(currencyCode)
                  .bankBuyPrice(bankBuy)
                  .bankSellPrice(bankSell)
                  .priceUpdateAt(lastUpdate)
                  .saveTheRecord()

        case e: Failure => e
      }

      newRecord match {
        case Failure(msg, error, _) => error.foreach(_.printStackTrace())
        case _ =>
      }
    }

    rateHTML.failed.foreach(_.printStackTrace)
  }
}

class Currency extends MongoRecord[Currency] with ObjectIdPk[Currency] {
  def meta = Currency

  val code = new StringField(this, 3)
 
  val bankSellPrice = new DecimalField(this, 0)
  val bankBuyPrice = new DecimalField(this, 0)
  val priceUpdateAt = new DateTimeField(this)
}
