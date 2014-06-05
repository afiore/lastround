package com.lastroundapp.data

import akka.event.LoggingAdapter

import spray.json._
import spray.httpx.SprayJsonSupport

object Responses {
  sealed trait ApiError {
    val message: String
  }
  case class InvalidAuth(message:String)       extends ApiError
  case class ParamError(message:String)        extends ApiError
  case class EndpointError(message:String)     extends ApiError
  case class NotAuthorised(message:String)     extends ApiError
  case class RateLimitExceeded(message:String) extends ApiError
  case class Deprecated(message:String)        extends ApiError
  case class ServerError(message:String)       extends ApiError
  case class OtherError(message:String)        extends ApiError

  sealed trait FoursquareResponse[T]
  case class ResponseOK[T: JsonFormat](results: T) extends FoursquareResponse[T]
  case class ResponseError[Nothing](err: ApiError) extends FoursquareResponse[Nothing]

  trait ResponseHandler {
    def okOrElse[T](resp: FoursquareResponse[T])(onOk: T => Unit)(onError: ApiError => Unit): Unit = resp match {
      case ResponseOK(result) => onOk(result)
      case ResponseError(err) => onError(err)
    }
  }

  object FSResponseJsonProtocol extends DefaultJsonProtocol
                                with SprayJsonSupport {

    implicit object ApiError2Json extends JsonFormat[ApiError] {
      def write(err: ApiError): JsValue = JsObject("error"  -> JsString(err.message))
      def read(v: JsValue): ApiError = ???
    }

    implicit def FsResp2Json[T: JsonFormat] = new RootJsonFormat[FoursquareResponse[T]] {
      def write(resp:FoursquareResponse[T]):JsValue = resp match {
        case ResponseOK(value)  => JsObject("result" -> value.toJson)
        case ResponseError(err) => err.toJson
      }

      def read(v:JsValue):FoursquareResponse[T] = {
        v.asJsObject.getFields("meta", "response") match {
          case Seq(meta:JsObject, resp:JsObject) =>
            parseResponse(meta, resp)
          case _ =>
            ResponseError(OtherError("Cannot find \"meta\" attribute"))
        }
      }
      private def parseResponse(meta:JsObject, resp:JsObject):FoursquareResponse[T] = {
         meta.getFields("code", "errorType", "errorDetail") match {
           case Seq(JsNumber(n)) if n == 200 =>
             ResponseOK(implicitly[JsonFormat[T]].read(resp))
           case Seq(_, JsString(errType), JsString(errDetail)) =>
             ResponseError(parseApiError(errType, errDetail))
           case other => throw new DeserializationException("Could not parse 'meta' attribute")
         }
      }
      private def parseApiError(errType:String, msg:String):ApiError = errType match {
          case "param_error"         => ParamError(msg)
          case "invalid_auth"        => InvalidAuth(msg)
          case "endpoint_error"      => EndpointError(msg)
          case "not_authorized"      => NotAuthorised(msg)
          case "rate_limit_exceeded" => RateLimitExceeded(msg)
          case "deprecated"          => Deprecated(msg)
          case "server_error"        => ServerError(msg)
          case "other"               => OtherError(msg)
      }
    }
  }
}
