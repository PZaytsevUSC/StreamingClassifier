package backend.connector

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp
import akka.stream.scaladsl.Tcp.OutgoingConnection
import backend.bidiflowprotocolstack.{CodecStage, FramingStage}
import backend.connector.Connector.Endpoint
import backend.connector.Connector.Messages.Startup
import backend.connectormanager.CMMCommands.SchemaSaved
import backend.messages.ConnectorMsg.{ConnectorRegistered, SaveSchema, StreamRequestStart}
import backend.messages._
import backend.schema.Schema

import scala.collection.JavaConverters._
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

  def start(cmdId: String, connectorId: String)(implicit sys: ActorSystem) = {
    sys.actorOf(props_connector(cmdId, connectorId))
  }

  object Messages {

    trait ConnectionMessage
    // Messages to himself
    case class SuccessfullyConnected(connection: OutgoingConnection) extends ConnectionMessage
    case object ConnectionFailed extends ConnectionMessage
    case object Startup


  }
}

class Connector(cmId: String, connectorId: String) extends Actor with ActorLogging{

  implicit val sys = context.system
  implicit val disp = context.dispatcher

  val cfg = sys.settings.config
  val servers:List[String] = sys.settings.config.getStringList("connector.servers.enabled").asScala.toList
  val config = servers.head
  val endpoint = Endpoint(sys.settings.config.getString(s"connector.servers.$config.host"), sys.settings.config.getString(s"connector.servers.$config.port").toInt)


  implicit val mat = ActorMaterializer()
  private var currentSchema: Option[Schema] = None : Option[Schema]
  private val parent: ActorRef = context.parent
  private def startServer() = {

    Tcp().bind(endpoint.host, endpoint.port) runForeach {

      connection => connection handleWith(ConnectorPublicationStage() join (CodecStage().reversed atop FramingStage().reversed))
    }
  }


  def receive: Receive = {

    case Startup =>
      log.warning("{}->{} starting at {}:{}", cmId, connectorId, endpoint.host, endpoint.port)
      startServer()

    case StreamRequestStart(`cmId`, `connectorId`) =>
      sender() ! ConnectorRegistered

    case SaveSchema(schema_id, schema, cmId, connectorId) =>
      currentSchema = Some(schema)
      sender() ! SchemaSaved

    case StreamRequestStart(cmId, connectorId) =>
      log.warning("Ignoring request for {} {}. Connector is responsible for {} {}", cmId, connectorId, this.cmId, this.connectorId)

  }

}