package com.lastroundapp

import com.typesafe.config.ConfigFactory

package object actors {
  val akkaConfig = ConfigFactory.parseString("""
    akka.loggers = ["akka.testkit.TestEventListener"]
    akka.loglevel = "ERROR"
    """)
}
