import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/**
 * Input arguments. That's right, you gotta write code, that's how lazy this program is!
 */

// Your current location
val LATITUDE = 44.9778
val LONGITUDE = -93.2650

// Item links that you want to scan
val LINKS = listOf(
  "https://www.walgreens.com/store/c/huggies-natural-care-sensitive-baby-wipes,-unscented,-3-flip--top-packs-(168-sheets-total)-fragrance-free/ID=prod6144346-product",
  "https://www.walgreens.com/store/c/huggies-natural-care-sensitive-baby-wipes,-unscented,-1-refill-pack-(184-wipes-total)-fragrance-free/ID=prod6107657-product"
)

/**
 * Ignore the shamefully hacky code below, please.
 */

val OKHTTP_CLIENT = OkHttpClient()
val MOSHI = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
val DEBUG = false

fun main() {
  LINKS.forEach { link ->
    val item = getItem(link)

    if (item == null) {
      println("Could not load data for $link")
    } else {
      val results = query(item.sku, item.pln, LATITUDE, LONGITUDE)

      if (results.isEmpty()) {
        println("${item.name} is not in stock.")
      }
      else {
        println("!!! ${item.name} is in stock !!!")
        results.forEach { listing ->
          println("${listing.inventory.inventoryCount} in store, ${listing.distance}mi away @ ${listing.store.address}")
        }
      }
    }

    println()
  }
}

fun getItem(url: String): Item? {
  try {
    val response = OKHTTP_CLIENT.newCall(Request.Builder().url(url).build()).execute()
    val body = response.body!!.string()

    // Not the prettiest parsing code ever, but it works, who cares
    val name = "<meta property=\"og:title\" content=\"(.+)\"/>".toRegex().find(body)?.groupValues?.get(1)
    val pln = "\"pln\":\"(\\d+)\"".toRegex().find(body)?.groupValues?.get(1)
    val sku = "\"skuId\":\"(.+?)\"".toRegex().find(body)?.groupValues?.get(1)

    return Item(
      sku = sku ?: return null,
      pln = pln ?: return null,
      name = name ?: return null
    )
  } catch (e: IOException) {
    if (DEBUG) println("Failed to get item at $url:\n$e")
    return null
  }
}

fun query(sku: String, pln: String, lat: Double, lng: Double): List<Listing> {
  val requestAdapter = MOSHI.adapter<WalgreensRequest>(WalgreensRequest::class.java)

  val request = WalgreensRequest(
    skuId = sku,
    plnId = pln,
    lat = lat.toString(),
    lng = lng.toString()
  )
  val requestJson = requestAdapter.toJson(request)

  val response: Response
  try {
    response = OKHTTP_CLIENT.newCall(
      Request.Builder()
        .url("https://www.walgreens.com/locator/v1/search/stores/inventory?requestor=pdpui")
        .method("POST", requestJson.toRequestBody())
        .addHeader("content-type", "application/json")
        .build()
    ).execute()
  } catch (e: IOException) {
    if (DEBUG) println("Failed tquery sku $sku:\n$e")
    return emptyList()
  }

  val resultAdapter = MOSHI.adapter<Result>(Result::class.java)
  return resultAdapter.fromJson(response.body!!.source())!!.results
    .sortedBy { it.distance }
}

// Workaround for Gradle application plugin
class Main