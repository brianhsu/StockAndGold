package code.model

import code.lib._

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._
import java.util.Calendar

object CurrencyInHand extends CurrencyInHand with MongoMetaRecord[CurrencyInHand]

class CurrencyInHand extends MongoRecord[CurrencyInHand] with ObjectIdPk[CurrencyInHand] {
  def meta = CurrencyInHand

  val userID = new StringField(this, 28)
  val code = new StringField(this, 3)
  val buyPrice = new DecimalField(this, 0)
  val quantity = new DecimalField(this, 0)
  val buyDate = new DateTimeField(this)

  val targetEarning = new IntField(this)
  val targetLoose = new IntField(this)
  val updateAt = new DateTimeField(this)
  val isNotified = new BooleanField(this, false)
  val notifiedAt = new OptionalDateTimeField(this)

  def notifySell() {

    for {
      user <- User.find(userID.get)
      totalPrice = buyPrice.get * quantity.get
      newPrice <- Currency.find("code", code.toString)
      newTotalPrice = newPrice.bankBuyPrice.get * quantity.get
    } {

      val isEarningEnough = newTotalPrice - totalPrice >= targetEarning.get
      val isLooseEnough = totalPrice - newTotalPrice >= targetLoose.get
      val difference = newTotalPrice - totalPrice
      val currencyName = 
        Currency.currencyCodeToName.get(code.toString)
                .getOrElse(code)

      if (isEarningEnough || isLooseEnough) {
        val message =
          s"成本為 ${totalPrice} 的 $currencyName，" +
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

