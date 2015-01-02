package bootstrap.liftweb

import com.mongodb.MongoClient
import net.liftweb.util.DefaultConnectionIdentifier
import net.liftweb.http.LiftRules
import net.liftweb.mongodb.MongoDB
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc.EarlyResponse

import org.bone.soplurk.api._

import net.liftweb.http.SessionVar

object Login {

  object CurrentPlurkAPI extends SessionVar[Option[PlurkAPI]](None)

  import net.liftweb.http.S
  import scala.util.Success

  val appKey = "oGjxYZZMHfPE"
  val appSecret = "DpVRPOBriTqHMZIFjxjgpuTDk55LiIlK"
  val callback = "http://localhost:8081/authPlurk"

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

    val authResult = for {
      plurkAPI    <- CurrentPlurkAPI.get
      verifyCode  <- S.param("oauth_verifier").toOption
      verifyOK    <- plurkAPI.authorize(verifyCode).toOption
    } yield "OK"

    authResult match {
      case Some(_) => S.redirectTo("/dashbaord")
      case None => S.redirectTo("/", () => S.error("驗證碼錯誤，無法登入，請稍候再試"))
    }
  }
}

class Boot {

  val siteMap = SiteMap(
    Menu("Index") / "index",
    Menu("Static") / "static" / **,
    Menu("Login") / "login" >> Login.redirectToPlurk,
    Menu("Auth") / "authPlurk" >> Login.checkAuthCode

  )

  def boot = {
    MongoDB.defineDb(DefaultConnectionIdentifier, new MongoClient, "stockAndGold")

    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
    LiftRules.addToPackages("code")
    LiftRules.setSiteMap(siteMap)
  }
}
