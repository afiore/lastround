package com.lastroundapp.data

import spray.json._
import spray.http.Uri
import spray.httpx.SprayJsonSupport

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
