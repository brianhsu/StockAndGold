package code.model

import code.lib._
import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._
import java.util.Calendar

object CurrencyToBuy extends CurrencyToBuy with MongoMetaRecord[CurrencyToBuy]

class CurrencyToBuy extends MongoRecord[CurrencyToBuy] with ObjectIdPk[CurrencyToBuy] {
  def meta = CurrencyToBuy

  val userID = new StringField(this, 24)
  val code = new StringField(this, 20)
  val unitPrice = new DecimalField(this, 2)
  val isNotified = new BooleanField(this, false)
  val notifiedAt = new OptionalDateTimeField(this, None)
  val updateAt = new DateTimeField(this)

  def notifyBuy() {

    for {
      user <- User.find(userID.get)
      newPrice <- Currency.find("code", code.toString)
      bankSellPrice = newPrice.bankSellPrice.get
    } {

      val currencyName = 
        Currency.currencyCodeToName.get(code.toString)
                .getOrElse(code)

      if (bankSellPrice <= unitPrice.get) {
        val message = 
          s"$currencyName 的銀行賣出價為 $bankSellPrice ，已達設定的買入點 $unitPrice"

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


