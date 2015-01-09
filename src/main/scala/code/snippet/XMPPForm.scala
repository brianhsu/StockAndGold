package code.snippet

import code.lib.DateToCalendar._
import code.model._
import java.text.SimpleDateFormat
import net.liftweb.common._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JE
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.util._
import scala.xml.Text

class XMPPForm {
  private var xmppUsername: String = _
  private var xmppServer: String = _

  private def saveToDatabase() = {
    for {
      user     <- User.currentUser.get
      username <- Option(xmppUsername)
      server   <- Option(xmppServer)
      xmppAddress <- user.xmppAddress(s"${username}@${server}").saveTheRecord()
    } {
      S.notice(s"已將 XMPP 通知設定至 $username@$server")
    }
    Noop
  }

  private def setupXMPPUsername(username: String) = {
    xmppUsername = username
    saveToDatabase()
  }
  private def setupXMPPServer(server: String) = {
    xmppServer = server
    saveToDatabase()
  }
  
  def render = {
    "@xmppUsername" #> SHtml.ajaxText(xmppUsername, false, setupXMPPUsername _) &
    "@xmppServer" #> SHtml.ajaxText(xmppServer, false, setupXMPPServer _)
  }
}

