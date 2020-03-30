data class Item(
  val sku: String,
  val pln: String,
  val name: String
)

data class WalgreensRequest(
  val skuId: String,
  val plnId: String,
  val lat: String,
  val lng: String,
  val requestType: String = "findAtYourLocal",
  val inStockOnly: String = "true",

  // I don't know what these represent, but they are necessary for the search to work
  val p: String = "1",
  val s: String = "100",
  val r: String = "10"
)

data class Result(
  val results: List<Listing>
)

data class Listing(
  val storeNumber: String,
  val distance: Double,
  val store: Store,
  val inventory: Inventory
)

data class Store(
  val address: Address
)

data class Address(
  val street: String,
  val city: String,
  val zip: String,
  val state: String
) {
  override fun toString() = "$street, $city, $zip, $state"
}

data class Inventory(
  val inventoryCount: String
)
