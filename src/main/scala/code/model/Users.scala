package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._

object User extends User with MongoMetaRecord[User]

class User extends MongoRecord[User] with ObjectIdPk[User] {
  def meta = User

  val email = new EmailField(this, 200)
  val plurkTokenKey = new StringField(this, 100)
  val plurkTokenSecret = new StringField(this, 100)
}


