package code.lib

import org.bone.soplurk.api._
import org.bone.soplurk.constant.Qualifier
import code.model._

import net.liftweb.util.Props

object PrivateMessanger {

  private val appKey = Props.get("PLURK_APIKEY").getOrElse("")
  private val appSecret = Props.get("PLURK_APISECRET").getOrElse("")
  private val accessToken = Props.get("PLURK_ACCESS_TOKEN").getOrElse("")
  private val accessSecret = Props.get("PLURK_ACCESS_SECRET").getOrElse("")

  val plurkAPI = PlurkAPI.withAccessToken(appKey, appSecret, accessToken, accessSecret)

  def sendMessage(user: User, message: String) {
    plurkAPI.Timeline.plurkAdd(message, Qualifier.Says, List(user.plurkUserID.get))
  }
}
