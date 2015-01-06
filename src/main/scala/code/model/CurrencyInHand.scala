package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._

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
}

