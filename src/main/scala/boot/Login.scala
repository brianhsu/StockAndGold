package bootstrap.liftweb

import org.bone.soplurk.api._

import code.model._
import net.liftweb.common._
import net.liftweb.http.S
import net.liftweb.http.SessionVar
import net.liftweb.sitemap.Loc.EarlyResponse
import scala.util.Success
import net.liftweb.util.Props

object Login {

  object CurrentPlurkAPI extends SessionVar[Option[PlurkAPI]](None)

  val appKey = Props.get("PLURK_APIKEY").getOrElse("InvalidKey")
  val appSecret = Props.get("PLURK_APISECRET").getOrElse("InvalidKey")
  val callback = Props.get("CALLBACK", S.hostAndPath + "/authPlurk")

  println(Props.mode)
  println("appKey:" + appKey)
  println("callback:" + callback)

  val redirectToPlurk = EarlyResponse { () =>
    val plurkAPI = PlurkAPI.withCallback(appKey, appSecret, callback)
    plurkAPI.getAuthorizationURL match {
      case Success(url) => 
        CurrentPlurkAPI(Some(plurkAPI))
        S.redirectTo(url)
      case _ => 
        S.redirectTo("/", () => S.error("無法取得噗浪登入網址，請稍候再試"))
    }
  }

  val checkAuthCode = EarlyResponse { () =>

    def updateUserTokenInfo(plurkAPI: PlurkAPI): Option[User] = {

      def insertOrUpdate(plurkUserID: Long, nickname: String, token: String, secret: String): Box[User] = {

        User.find("nickname", nickname) match {
          case Empty => 

            User.createRecord
                .nickname(nickname)
                .plurkUserID(plurkUserID)
                .plurkToken(token)
                .plurkSecret(secret)
                .saveTheRecord()

          case Full(user) => 

            user.plurkToken(token)
                .plurkSecret(secret)
                .saveTheRecord()

          case failure => failure
        }
      }

      for {
        (userInfo, _, _) <- plurkAPI.Users.currUser.toOption
        token <- plurkAPI.getAccessToken
        nickname = userInfo.basicInfo.nickname
        plurkUserID = userInfo.basicInfo.id
        currentUser <- insertOrUpdate(plurkUserID, nickname, token.getToken, token.getSecret)
      } yield currentUser
    }

    val authResult = for {
      plurkAPI    <- CurrentPlurkAPI.get
      verifyCode  <- S.param("oauth_verifier").toOption
      verifyOK    <- plurkAPI.authorize(verifyCode).toOption
      currentUser <- updateUserTokenInfo(plurkAPI)
    } yield currentUser

    authResult match {
      case Some(user) => 
        User.currentUser(Some(user))
        S.redirectTo("/dashboard")
      case None => 
        User.currentUser(None)
        S.redirectTo("/", () => S.error("驗證碼錯誤，無法登入，請稍候再試"))
    }
  }
}

