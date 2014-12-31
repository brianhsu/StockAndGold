package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._

object Currency extends Currency with MongoMetaRecord[Currency]

class Currency extends MongoRecord[Currency] with ObjectIdPk[Currency] {
  def meta = Currency

  val code = new StringField(this, 3)
 
  val officalPrice = new OptionalDecimalField(this, None, 3)
  val bankSellPrice = new OptionalDecimalField(this, None, 3)
  val bankBuyPrice = new OptionalDecimalField(this, None, 3)
  val priceUpdateAt = new OptionalDateTimeField(this, None)
}
