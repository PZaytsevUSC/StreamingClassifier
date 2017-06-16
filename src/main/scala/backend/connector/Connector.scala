package backend.connector

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import backend.connector.Connector.Endpoint
import backend.messages.ConnectorMsg.{ConnectorRegistered, StreamRequestStart}
import backend.messages._
/**
  * Created by pzaytsev on 4/9/17.
  */

//Principles of operation:

// can be of several types depending on what it connects to

// should be able to use persistence module
// in case of shutdown it should restore a state it was in


// It would be able to be identified by: connectorID, connectorManagerID, Optional EndPoint
object Connector {

  // endpoint is where a connector is located
  case class Endpoint(host: String, port: Int)

  // props should include ids and endpoint
  def props_connector(cmId: String, connectorId: String, endpoint: Option[Endpoint]): Props = Props(new Connector(cmId, connectorId, endpoint))


  object Messages {

    trait ConnectionMessage
    // Messages to himself
    case class SuccessfullyConnected(connection: OutgoingConnection) extends ConnectionMessage
    case object ConnectionFailed extends ConnectionMessage



  }
}

class Connector(cmId: String, connectorId: String, endpoint: Option[Endpoint]) extends Actor with ActorLogging{

  implicit val sys = context.system
  implicit val disp = context.dispatcher

  // should filter on datatype and return boolean
  // var filters: List[PartialFunction[Int, Boolean]] = ???
  // should be a schema expected against which to verify and for which to classify
  // var current_schema = ???
  // a list of models to choose a streaming classification for
  // it should consume a datashape and output a new datashape + a new classification
  // var models: Map[String, PartialFunction[String, String]] = ???
  // should affect the ingestion stage of a streaming logic. Cassandra -> Cassandra type ingestion, AMQP -> AMQP type, etc
  // val source_type = ???

  def receive: Receive = {

    case StreamRequestStart(`cmId`, `connectorId`) =>
      sender() ! ConnectorRegistered

    case StreamRequestStart(cmId, connectorId) =>
      log.warning("Ignoring request for {} {}. Connector is responsible for {} {}", cmId, connectorId, this.cmId, this.connectorId)

  }

}