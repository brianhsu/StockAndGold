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
  val buyDate = new DateField(this)
  val buyPrice = new DecimalField(this, 0)
  val buyFee = new IntField(this, 0)
  val quantity = new DecimalField(this, 0)

  val targetEarning = new OptionalIntField(this)
  val targetLoose = new OptionalIntField(this)
  val updateAt = new DateTimeField(this)
}

