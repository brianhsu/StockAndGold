package bootstrap.liftweb

import com.mongodb.MongoClient
import net.liftweb.util.DefaultConnectionIdentifier
import net.liftweb.http.LiftRules
import net.liftweb.mongodb.MongoDB

class Boot {

  MongoDB.defineDb(DefaultConnectionIdentifier, new MongoClient, "stockAndGold")

  LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
  LiftRules.addToPackages("code")
}
