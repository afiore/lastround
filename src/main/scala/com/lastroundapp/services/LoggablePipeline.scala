package com.lastroundapp.services

import akka.actor._
import akka.event.LoggingAdapter

import scala.concurrent.{Future, ExecutionContext}

import spray.http.{HttpRequest, HttpResponse, HttpHeaders, MediaTypes}
import spray.httpx.encoding.{Gzip, Deflate}
import spray.httpx.unmarshalling._

import spray.client.pipelining._
import HttpHeaders._
import MediaTypes._
import spray.client.PipelineException

trait LoggablePipeline {
  import spray.httpx.RequestBuilding._

  val log: LoggingAdapter

  def pipeline[T: FromResponseUnmarshaller](req:HttpRequest)(
    implicit ac:ActorContext, ec:ExecutionContext): Future[T] = {

    val pipe =
      (encode(Gzip)
        ~> addHeader(Accept(`application/json`))
        ~> logRequest
        ~> sendReceive
        ~> decode(Deflate)
        ~> logResponse
        ~> _unmarshal )

    pipe(req)
  }

  private def _unmarshal[T: FromResponseUnmarshaller]: HttpResponse => T =
    r => r.as[T] match {
      case Right(value) =>
        value
      case _ =>
        throw new PipelineException("Cannot parse response")
    }

  private val logRequest: HttpRequest => HttpRequest =
    { r => log.debug(s"Issuing request:{}", r); r }

  private val logResponse: HttpResponse => HttpResponse =
    { r => log.debug(s"Got response: {}", r); r }
}