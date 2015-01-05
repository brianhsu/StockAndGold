package code.lib

object PriceFormatter {

  def apply(price: BigDecimal) = {
    if (price > 0) {
      <span style="color: red">{"+" + price}</span>
    }
    else {
      <span style="color: green">{price}</span>
    }
  }
}
