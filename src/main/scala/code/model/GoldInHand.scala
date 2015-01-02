package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._

object GoldInHand extends GoldInHand with MongoMetaRecord[GoldInHand]

class GoldInHand extends MongoRecord[GoldInHand] with ObjectIdPk[GoldInHand] {
  def meta = GoldInHand

  val userID = new StringField(this, 24)

  val buyPrice = new DecimalField(this, 0)
  val quantity = new DecimalField(this, 0)
  
  val earningTarget = new OptionalIntField(this)
  val lostTarget = new OptionalIntField(this)
  val updateAt = new DateTimeField(this)
}

