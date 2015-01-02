package code.model

import code.lib._

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._

import java.text.SimpleDateFormat
import java.util.Calendar

import scala.concurrent.ExecutionContext.Implicits.global

object Currency extends Currency with MongoMetaRecord[Currency] {

  def updateNewValue = {

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

    val rateHTMLFuture = DataGetter("http://rate.bot.com.tw/Pages/Static/UIP003.zh-TW.htm")

    for {
      htmlData <- rateHTMLFuture
      csvURL <- getCsvURL(htmlData)
      csvData <- DataGetter(csvURL)
      lastUpdate = getLastUpdateTime(csvURL)
      record <- csvData.split("\n").drop(1)
    } {
      val columns = record.split("\\s+").toList
      val currencyCode = columns(1).trim
      val bankBuy = BigDecimal(columns(3).trim)
      val bankSell = BigDecimal(columns(13).trim)

      Currency.delete("code", currencyCode)
      Currency.createRecord
              .code(currencyCode)
              .bankBuyPrice(bankBuy)
              .bankSellPrice(bankSell)
              .priceUpdateAt(lastUpdate)
              .saveTheRecord()

    }
  }
}

class Currency extends MongoRecord[Currency] with ObjectIdPk[Currency] {
  def meta = Currency

  val code = new StringField(this, 3)
 
  val bankSellPrice = new DecimalField(this, 0)
  val bankBuyPrice = new DecimalField(this, 0)
  val priceUpdateAt = new DateTimeField(this)
}
