package code.model

import code.lib._
import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._
import java.util.Calendar

object StockToBuy extends StockToBuy with MongoMetaRecord[StockToBuy]

class StockToBuy extends MongoRecord[StockToBuy] with ObjectIdPk[StockToBuy] {
  def meta = StockToBuy

  val userID = new StringField(this, 24)
  val stockID = new StringField(this, 20)
  val unitPrice = new DecimalField(this, 2)
  val isNotified = new BooleanField(this, false)
  val notifiedAt = new OptionalDateTimeField(this, None)
  val updateAt = new DateTimeField(this)

  def notifyBuy() {

    for {
      user <- User.find(userID.get)
      newPrice <- Stock.find("code", stockID.toString)
      currentPrice = newPrice.currentPrice.get
    } {

      val stockName = Stock.stockCodeToName.get(stockID.toString).getOrElse(stockID)

      if (currentPrice <= unitPrice.get) {
        val message = 
          s"$stockName 的成交價價為 $currentPrice ，已達設定的買入點 $unitPrice"

        if (user.nickname.get == "brianhsu") {
          PrivateMessanger.sendMessage(user, message)
          this.isNotified(true).notifiedAt(Calendar.getInstance).saveTheRecord()
        } else {

          val newPlurk = user.postPlurk(message)
          user.xmppAddress.get.foreach(address => XMPPMessanger.send(address, message))
          newPlurk.foreach { plurk =>
            this.isNotified(true).notifiedAt(Calendar.getInstance).saveTheRecord()
          }
        }
      }
    }
  }

}

