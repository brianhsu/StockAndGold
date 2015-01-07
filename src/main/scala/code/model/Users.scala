package code.model

import org.bone.soplurk.api._
import org.bone.soplurk.constant._


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

  val buyGoldAt = new OptionalIntField(this, None)
  val isBuyGoldNotified = new BooleanField(this, false)
  val xmppAddress = new OptionalEmailField(this, 200)

  def postPlurk(message: String) = {
    val appKey = "oGjxYZZMHfPE"
    val appSecret = "DpVRPOBriTqHMZIFjxjgpuTDk55LiIlK"
    val plurkAPI = PlurkAPI.withAccessToken(appKey, appSecret, plurkToken.get, plurkSecret.get)
    plurkAPI.Timeline.plurkAdd(message, Qualifier.Says, List(plurkUserID.get))
  }
}


