package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._
import net.liftweb.common._

import code.lib._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.XML
import scala.xml.NodeSeq
import scala.xml.Node
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.concurrent.Future

object Gold extends Gold with MongoMetaRecord[Gold] {

  def updateNewPrice() = {

    def getGoldSavingHTML(lines: Seq[String]): Option[NodeSeq] = {

      def isGoldSavingRow(line: String) = {
        line.contains("goldName0") && 
        line.contains("黃金存摺")
      }

      val xmlLineOption = lines.filter(isGoldSavingRow).headOption.map(_.trim)
      xmlLineOption.map { xmlLine => XML.loadString(s"<root>$xmlLine</root>") \\ "tr" }
    }

    def getPriceFromTR(trNode: Node) = (trNode \\ "td").last.text.toInt

    def getLastUpdateTime(lines: Seq[String]): Option[Calendar] = {
      val dateLineOption = lines.filter(_ contains "掛牌時間").headOption
      dateLineOption.map { line =>
        val dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm")
        val calendar = Calendar.getInstance
        calendar.setTime(dateFormatter.parse(line.takeRight(17)))
        calendar
      }
    }

    val htmlData = DataGetter("http://rate.bot.com.tw/Pages/Static/UIP005.zh-TW.htm")

    for {
      content <- htmlData
      lines = content.split("\n")
      goldSavingHTML <- getGoldSavingHTML(lines)
      lastUpdateTime <- getLastUpdateTime(lines)
    } {

      val List(bankSell, bankBuy) = goldSavingHTML.map(getPriceFromTR)

      val newRecord = Gold.find("bankName", "TaiwanBank") match {

        case Full(record) => 
          Gold.bankSellPrice(bankSell)
              .bankBuyPrice(bankBuy)
              .priceUpdateAt(lastUpdateTime)
              .saveTheRecord()

        case Empty =>
          Gold.createRecord
              .bankName("TaiwanBank")
              .bankSellPrice(bankSell)
              .bankBuyPrice(bankBuy)
              .priceUpdateAt(lastUpdateTime)
              .saveTheRecord()

        case e: Failure => e

      }

      newRecord match {
        case Failure(msg, exception, _) => exception.foreach(_.printStackTrace)
        case _ => 
      }
    }

    htmlData.failed.foreach(_.printStackTrace)
  }
}

class Gold extends MongoRecord[Gold] with ObjectIdPk[Gold] {
  def meta = Gold

  val bankName = new StringField(this, 20)
  val bankSellPrice = new DecimalField(this, 0)
  val bankBuyPrice = new DecimalField(this, 0)
  val priceUpdateAt = new DateTimeField(this)
}


