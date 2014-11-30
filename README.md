# Lastround

A Spray/Akka app using the Foursquare API to discover venues open until Late.
Based on the user's location and the current time, Lastround recommends a list of
nearby venues.

The application consists of a Scala back-end handling the communication with
Foursquare, and a AngularJS front-end powering the user interface.
As the Foursquare API relies on OAuth, a user must have an account on the service
in order to use the application.

## Setup

To run an instance of Lastround [first register a Foursquare app](https://foursquare.com/developers/register)
and get hold of your own credentials (client id, and secret). At this point,
create a overrides.conf file and place it into the project root folder.
(see 'overrides.conf.example' for a reference).

# Building and running the app

A jar archive containing Lastround and all its dependencies can be built by executing
the `sbt assembly` task. The task will execute the project test suite and, when
no errors occur, produce a jar file. Once built, the jar can be executed by running:

    java -Doverrides.conf=overrides.conf -jar /target/scala-2.11/lastround-assembly-1.0-SNAPSHOT.jar

The application UI can be accessed by visiting http://localhost:8080.
