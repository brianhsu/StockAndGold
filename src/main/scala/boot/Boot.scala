package bootstrap.liftweb

import com.mongodb.MongoClient

import net.liftweb.util.DefaultConnectionIdentifier
import net.liftweb.http.LiftRules
import net.liftweb.mongodb.MongoDB
import net.liftweb.sitemap._

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
