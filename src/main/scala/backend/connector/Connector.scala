package backend.connector

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp
import akka.stream.scaladsl.Tcp.OutgoingConnection
import backend.bidiflowprotocolstack.{CodecStage, FramingStage}
import backend.connector.Connector.Endpoint
import backend.connectormanager.CMMCommands.SchemaSaved
import backend.messages.ConnectorMsg.{ConnectorRegistered, SaveSchema, StreamRequestStart}
import backend.messages._
import backend.schema.Schema
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
  def props_connector(cmId: String, connectorId: String): Props = Props(new Connector(cmId, connectorId))


  object Messages {

    trait ConnectionMessage
    // Messages to himself
    case class SuccessfullyConnected(connection: OutgoingConnection) extends ConnectionMessage
    case object ConnectionFailed extends ConnectionMessage



  }
}

class Connector(cmId: String, connectorId: String) extends Actor with ActorLogging{

  implicit val sys = context.system
  implicit val disp = context.dispatcher
  implicit val mat = ActorMaterializer()
  private var currentSchema: Option[Schema] = None : Option[Schema]
  private val parent: ActorRef = context.parent
  private def startServer(host: String, port: Int) = {
    val host = "localhost"
    val port = 8881

    Tcp().bind(host, port) runForeach {

      connection => connection handleWith(ConnectorPublicationStage() join (CodecStage().reversed atop FramingStage().reversed))
    }
  }


  def receive: Receive = {

    case StreamRequestStart(`cmId`, `connectorId`) =>
      sender() ! ConnectorRegistered

    case SaveSchema(schema_id, schema, cmId, connectorId) =>
      currentSchema = Some(schema)
      sender() ! SchemaSaved

    case StreamRequestStart(cmId, connectorId) =>
      log.warning("Ignoring request for {} {}. Connector is responsible for {} {}", cmId, connectorId, this.cmId, this.connectorId)

  }

}