package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._


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
}

