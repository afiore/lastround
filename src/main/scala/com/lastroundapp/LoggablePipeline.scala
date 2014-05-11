package com.lastroundapp.services

import akka.actor._
import scala.concurrent.{Future, ExecutionContext}

import spray.http.{HttpRequest, HttpResponse, HttpMethods, HttpHeaders, MediaTypes}
import spray.httpx.encoding.{Gzip, Deflate}
import spray.httpx.unmarshalling.FromResponseUnmarshaller

import spray.client.pipelining._

import HttpMethods._
import HttpHeaders._
import MediaTypes._
import com.lastroundapp.data.Responses._

trait LoggablePipeline { self:ActorLogging =>
  import spray.httpx.RequestBuilding._

  def okOrLogError[T](resp: FoursquareResponse[T])(onOk: T => Unit) =
    okOrElse(resp)(onOk) { apiErr =>
      log.error(s"a Foursquare API error has occurred: $apiErr")
    }

  def okOrElse[T](resp: FoursquareResponse[T])(onOk: T => Unit)(onError: ApiError => Unit): Unit = resp match {
    case ResponseOK(result) => onOk(result)
    case ResponseError(err) => onError(err)
  }

  def pipeline[T: FromResponseUnmarshaller](req:HttpRequest)(
      implicit ac:ActorContext, ec:ExecutionContext): Future[T] = {

    val pipe =
      (encode(Gzip)
        ~> addHeader(Accept(`application/json`))
        ~> logRequest
        ~> sendReceive
        ~> decode(Deflate)
        ~> logResponse
        ~> unmarshal[T])

    pipe(req)
  }

  private val logRequest: HttpRequest => HttpRequest =
    { r => log.debug(s"Issuing request: $r"); r }

  private val logResponse: HttpResponse => HttpResponse =
    { r => log.debug(s"Got response: $r"); r }
}
