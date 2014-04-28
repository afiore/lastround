package com.lastroundapp.data

import spray.json._
import DefaultJsonProtocol._
import spray.http.Uri
import spray.http.Uri._
import spray.httpx.SprayJsonSupport

object Foursquare {
  private lazy val clientId    = "IKDHDEDAOMAQCQOHK3PZKGPR1BGJ4Y0Z2BLNSFPMATTHVE12"
  private lazy val secret      = "ADHON00XRPPDBSSVPA2TZCIJY0YH0FLGHP4HGWVTWAVM2VJI"
  private lazy val redirectUri = "http://localhost:8080/foursquare/login"

  val host      = Uri("https://foursquare.com")
  val authPath  = Path("/oauth2/authenticate")
  val tokenPath = Path("/oauth2/access_token")
  val defaultQ  = Map("redirect_uri" -> redirectUri,
                      "client_id"    ->  clientId)

  def authUri: Uri  =
    host.withPath(authPath)
      .withQuery(defaultQ ++ Map("response_type" -> "code"))

  def tokenUri(code:String): Uri =
    host.withPath(tokenPath)
      .withQuery(defaultQ ++ Map("grant_type"    -> "authorization_code",
                                 "code"          -> code,
                                 "client_secret" -> secret))
}

case class FSToken(token: String)

object FoursquareJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object FSTokeFormat extends RootJsonFormat[FSToken] {
    def write(t: FSToken):JsObject = JsObject(
      "access_token" -> JsString(t.token)
    )

    def read(value: JsValue):FSToken = {
      value.asJsObject.getFields("access_token") match {
        case Seq(JsString(t)) => FSToken(t)
        case _ => throw new DeserializationException("access_token expected")
      }
    }
  }
}
