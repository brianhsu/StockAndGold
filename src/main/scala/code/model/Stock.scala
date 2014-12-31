package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._

object Stock extends Stock with MongoMetaRecord[Stock]

class Stock extends MongoRecord[Stock] with ObjectIdPk[Stock] {
  def meta = Stock

  val code = new StringField(this, 20)
  val name = new StringField(this, 20)
  val inTaiwan50 = new BooleanField(this, false)
  val inTaiwan100 = new BooleanField(this, false)

  val currentPrice = new OptionalDecimalField(this, None, 3)
  val minPrice = new OptionalDecimalField(this, None, 3)
  val maxPrice = new OptionalDecimalField(this, None, 3)
  val closePrice = new OptionalDecimalField(this, None, 3)
  val priceUpdateAt = new OptionalDateTimeField(this, None)
}
