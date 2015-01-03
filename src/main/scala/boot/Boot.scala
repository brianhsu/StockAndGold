package bootstrap.liftweb

import code.comet._
import code.model.User

import com.mongodb.MongoClient

import net.liftweb.util.DefaultConnectionIdentifier
import net.liftweb.http.LiftRules
import net.liftweb.http.S
import net.liftweb.mongodb.MongoDB
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc.If

class Boot {

  private val needLogin = If(
    () => User.isLoggedIn, 
    () => S.redirectTo("/", () => S.error("請先登入"))
  )

  val siteMap = SiteMap(
    Menu("Index") / "index",
    Menu("Static") / "static" / **,
    Menu("Login") / "login" >> Login.redirectToPlurk,
    Menu("Auth") / "authPlurk" >> Login.checkAuthCode,
    Menu("Dashboard") / "dashboard" >> needLogin
  )

  def boot = {
    MongoDB.defineDb(DefaultConnectionIdentifier, new MongoClient, "stockAndGold")

    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
    LiftRules.addToPackages("code")
    LiftRules.setSiteMap(siteMap)
    GoldTable.init()
    StockTable.init()
  }
}
