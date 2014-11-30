package com.lastroundapp.data

import java.io.File

import com.typesafe.config.ConfigFactory

object Settings {
  protected val overrideFile = {
    val overridesPath = System.getProperty("config.overrides")
    if (overridesPath == null) throw new RuntimeException("Could not find property config.overrides.")
    new File(overridesPath)
  }

  private val conf = {
    val baseConf = ConfigFactory.load.withFallback(ConfigFactory.parseResources("application.conf"))
    if (overrideFile.exists)
      ConfigFactory.parseFile(overrideFile).withFallback(baseConf)
    else
      baseConf
  }

  val foursquareHost          = conf.getString("lastround.foursquare.host")
  val foursquareApiVersion    = conf.getInt("lastround.foursquare.api-version")
  val foursquareClientId      = conf.getString("lastround.foursquare.client-id")
  val foursquareSecret        = conf.getString("lastround.foursquare.secret")
  val foursquareRedirectUrl   = conf.getString("lastround.foursquare.redirect-url")
  val venueHoursWorkerTimeout = conf.getInt("lastround.venue-hours-worker.timeout")
  val venueSearcherTimeout    = conf.getInt("lastround.venue-searcher.timeout")
  val streamerTimeout         = conf.getInt("lastround.result-streamer.timeout")
}
