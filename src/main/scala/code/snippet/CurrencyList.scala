package code.snippet

import code.model.{Currency => MCurrency}
import net.liftweb.util.Helpers._

class CurrencyList {
  def render = {
    ".currencyListItem" #> MCurrency.currencyList.map { currencyInfo =>
      ".currencyListItem *" #> s"[${currencyInfo.code}] ${currencyInfo.name}" &
      ".currencyListItem [value]" #> currencyInfo.code
    }
  }
}
