package code.model

import code.lib._
import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._
import java.util.Calendar

object StockInHand extends StockInHand with MongoMetaRecord[StockInHand]

class StockInHand extends MongoRecord[StockInHand] with ObjectIdPk[StockInHand] {
  def meta = StockInHand

  val userID = new StringField(this, 24)

  val stockID = new StringField(this, 20)
  val buyDate = new DateTimeField(this)
  val buyPrice = new DecimalField(this, 0)
  val buyFee = new IntField(this, 0)
  val quantity = new DecimalField(this, 0)

  val targetEarning = new IntField(this)
  val targetLoose = new IntField(this)
  val updateAt = new DateTimeField(this)
  val isNotified = new BooleanField(this, false)
  val notifiedAt = new OptionalDateTimeField(this, None)

  def notifySell() {

    for {
      user <- User.find(userID.get)
      totalPrice = buyPrice.get * quantity.get
      newPrice <- Stock.find("code", stockID.toString)
      newTotalPrice = newPrice.currentPrice.get * quantity.get
    } {

      val isEarningEnough = newTotalPrice - totalPrice >= targetEarning.get
      val isLooseEnough = totalPrice - newTotalPrice >= targetLoose.get
      val difference = newTotalPrice - totalPrice
      val stockName = Stock.stockCodeToName.get(stockID.toString).getOrElse(stockID)

      if (isEarningEnough || isLooseEnough) {
        val message =
          s"成本為 ${totalPrice} 的 $stockName，" +
          s"目前市值為 $newTotalPrice ，價差為 $difference，" +
          s"已達設定停損 / 停益點 $targetLoose / $targetEarning 。"

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

