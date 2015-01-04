package code.lib

import org.bone.soplurk.api._
import org.bone.soplurk.constant.Qualifier
import code.model._

import net.liftweb.util.Props
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.ChatManager
import scala.util.Try
import com.plivo.helper.api.client._
import java.util.LinkedHashMap

object PrivateMessanger {

  private val appKey = Props.get("PLURK_APIKEY").openOr("")
  private val appSecret = Props.get("PLURK_APISECRET").openOr("")
  private val accessToken = Props.get("PLURK_ACCESS_TOKEN").openOr("")
  private val accessSecret = Props.get("PLURK_ACCESS_SECRET").openOr("")
  private val xmppUsername = Props.get("XMPP_USERNAME").openOr("")
  private val xmppPassword = Props.get("XMPP_PASSWORD").openOr("")
  private val smsAuthID = Props.get("SMS_AUTH_ID").openOr("")
  private val smsAuthToken = Props.get("SMS_AUTH_TOKEN").openOr("")

  val plurkAPI = PlurkAPI.withAccessToken(appKey, appSecret, accessToken, accessSecret)

  def sendSMSMessage(message: String) = Try {
    val api = new RestAPI(smsAuthID, smsAuthToken, "v1")
    val params = new LinkedHashMap[String, String]
    params.put("src", Props.get("SMS_FROM").openOr(""))
    params.put("dst", Props.get("SMS_TO").openOr(""))
    params.put("text", message)
    api.sendMessage(params)
  }

  def sendPlurkMessage(user: User, message: String) {
    plurkAPI.Timeline.plurkAdd(message, Qualifier.Says, List(user.plurkUserID.get))
  }

  def sendXMPPMessage(message: String) = Try {
    val connection = new XMPPTCPConnection("xmpp.jp")
    connection.connect()
    connection.login(xmppUsername, xmppPassword)
    ChatManager.getInstanceFor(connection)
               .createChat("brianhsu@xmpp.jp", null)
               .sendMessage(message)

    ChatManager.getInstanceFor(connection)
               .createChat("brianhsu.phone@xmpp.jp", null)
               .sendMessage(message)

    connection.disconnect()
  }

  def sendMessage(user: User, message: String) {
    println("Send private message....")
    sendSMSMessage(message)
    sendXMPPMessage(message)
    sendPlurkMessage(user, message)
  }
}
