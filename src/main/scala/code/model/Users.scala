package code.model

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field._
import net.liftweb.http.SessionVar

object User extends User with MongoMetaRecord[User] {
  
  object currentUser extends SessionVar[Option[User]](None)
  def isLoggedIn = !currentUser.get.isEmpty
}

class User extends MongoRecord[User] with ObjectIdPk[User] {
  def meta = User

  val plurkUserID = new LongField(this)
  val nickname = new StringField(this, 200)
  val plurkToken = new StringField(this, 100)
  val plurkSecret = new StringField(this, 100)
}


