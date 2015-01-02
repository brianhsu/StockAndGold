package code.lib

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.HttpClientBuilder
import java.nio.charset.StandardCharsets

object DataGetter {

  def apply(url: String): Future[String] = Future {

    val httpClient = HttpClientBuilder.create.build
    val httpRequest = new HttpGet(url)
    httpRequest.setHeader(
      "User-Agent", 
      "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0"
    )
    val httpResponse = httpClient.execute(httpRequest)
    val responseContent = EntityUtils.toString(httpResponse.getEntity, StandardCharsets.UTF_8)

    httpClient.close()
    responseContent
  }

}

