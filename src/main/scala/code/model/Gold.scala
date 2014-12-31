package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._


object Gold extends Gold with MongoMetaRecord[Gold]

class Gold extends MongoRecord[Gold] with ObjectIdPk[Gold] {
  def meta = Gold

  val name = new StringField(this, 20)

  val bankSellPrice = new OptionalDecimalField(this, None, 3)
  val bankBuyPrice = new OptionalDecimalField(this, None, 3)
  val priceUpdateAt = new OptionalDateTimeField(this, None)
}


