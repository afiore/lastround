package com.lastroundapp

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.routing.FromConfig
import akka.io.IO
import spray.can.Http

import com.lastroundapp.data.Endpoints.LatLon
import com.lastroundapp.services.FoursquareClientImpl
import com.lastroundapp.actors.{VenueSearcher, VenueHoursWorker}
import VenueSearcher.RunSearch


object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")


  val logger        = Logging.getLogger(system, "FoursquareClient")
  val fsClient      = new FoursquareClientImpl(logger)
  // create and start our service actor
  val service       = system.actorOf(Props[MyServiceActor], "demo-service")
  // create a VenueSearcher actor
  val workerPool    = system.actorOf(FromConfig.props(Props(classOf[VenueHoursWorker], fsClient)), "venue-hours-router")
  val venueSearcher = system.actorOf(Props(classOf[VenueSearcher], fsClient, workerPool), "venue-searcher")

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8080)
  // perform a search (just for fun)
  venueSearcher ! RunSearch(LatLon(51.545, -0.0553))
}