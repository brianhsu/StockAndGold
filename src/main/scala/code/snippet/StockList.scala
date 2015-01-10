package code.snippet

import code.model._
import net.liftweb.util.Helpers._

class StockList {
  def render = {
    ".stockListItem" #> Stock.stockTable.map { stockInfo =>
      ".stockListItem *" #> s"[${stockInfo.code}] ${stockInfo.name}" &
      ".stockListItem [value]" #> stockInfo.code
    }
  }
}

