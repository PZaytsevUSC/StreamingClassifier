package backend.connectormanager

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, FSM, IndirectActorProducer, PoisonPill, Props, Stash}
import akka.event.Logging
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import backend.connector.Connector
import backend.connector.Connector.Endpoint
import backend.connector.Connector.props_connector
import backend.messages.CMMsg._

import language.postfixOps
import akka.stream.scaladsl.{Flow, Source, Tcp}
import akka.stream.scaladsl.Tcp.{IncomingConnection, OutgoingConnection, ServerBinding}
import akka.util.ByteString
import backend.messages.ConnectorMsg.{SaveSchema, StreamRequestStart}
import backend.schema.Schema
import com.typesafe.config.ConfigFactory

import scala.util
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
/// Hierarchical structure

/// Should initiate some set of connectors -> monitors connectors like it's children

/// Gives underlying commands to start building and materializing flows

/// Should know the failures and how to handle


//// Dependency Injection

/// Firstly local implementation that clustered implementation

object CMMCommands {
  sealed trait CMMCommand
  case class ConnectTo(cmId: String, connectorId: String) extends CMMCommand
  case class SuccessfullyConnected(connection: OutgoingConnection) extends CMMCommand
  case object ConnectionFailed extends CMMCommand
  case object ConnectorDoesNotExist extends CMMCommand
  case object SchemaInitialized extends CMMCommand
  case object SchemaSaved extends CMMCommand
  case object UpdateEndpoints extends CMMCommand
}


object ConnectorManager {
  def props_self(cmId: String): Props = Props(new ConnectorManager(cmId))
}

// Waiting -> Add Connector -> Build pipiline -> Monitor
// need a singleton to work with config file..

class ConnectorManager(cmId: String) extends Actor with Stash with ActorLogging{

  import context.become
  import context.unbecome

  import CMMCommands._
  implicit val sys = context.system
  implicit val disp = context.dispatcher
  implicit val materializer =
    ActorMaterializer(ActorMaterializerSettings(context.system))

  private val port = sys.settings.config.getInt("connector_manager.port")
  private val host = sys.settings.config.getString("connector_manager.host")

  val servers = sys.settings.config.getStringList("connector.servers.enabled")
  val serverConfig = sys.settings.config.getConfig(s"connector.servers.${servers.iterator().next()}")
  private var connector_counter: Int = 0
  private var schemas: Map[Long, Schema] = Map.empty[Long, Schema]
  private var connectors: Map[String, ActorRef] = Map.empty[String, ActorRef]
  private var endpoints: Map[String, Endpoint] = Map.empty[String, Endpoint]
  private var connectors_backward: Map[ActorRef, String] = Map.empty[ActorRef, String]
  override def preStart(): Unit = log.info("ConnectorManager {} is up", cmId)
  override def postStop(): Unit = log.info("ConnectorManager {} is down", cmId)

  // disable consecutive calls to prestart
  override def postRestart(reason: Throwable): Unit = ()

  // prevent from stopping all the children -> should check a stash box on startup
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    postStop()
  }

  def suicide() = {
    context.stop(self)
  }

  def start_watching_connectors() = {
    context.children foreach {
      child => context.watch(child)
    }
  }

  def stop_watching_connectors() = {
    context.children foreach {
      child => context.unwatch(child)
    }
  }

  def stop_all_connectors() = {
    context.children foreach {
      child => context.stop(child)
    }
  }


  def receive: Receive = {
    case Initialize => sender() ! "Initialized"; become(connector_creator)
    case Destroy => context.stop(self)
    case _ => sender() ! "Non Initialized"
  }

  def connector_handler: Receive = {

    case SuccessfullyConnected(connection: OutgoingConnection) => {
      ///
    }

    case ConnectionFailed => {

    }
  }

  def connector_creator: Receive = {

    case streamReq @ StreamRequestStart(`cmId`, _) => {
      connectors.get(streamReq.connectorId) match {
        case Some(connector) => connector forward streamReq
        case None =>
          log.info("Creating a connector for {}", streamReq.connectorId)
          if (servers.iterator().hasNext) {

            val serverConfig = sys.settings.config.getConfig(s"connector.servers.${servers.iterator().next()}")
            servers.remove(0)
            val endpoint = Endpoint(serverConfig.getString("host"), serverConfig.getInt("port"))
            val connector = context.actorOf(props_connector(streamReq.cmId, streamReq.connectorId, Some(endpoint)))
            context.watch(connector)
            connectors += streamReq.connectorId -> connector
            endpoints += streamReq.connectorId -> endpoint
            connectors_backward += connector -> streamReq.connectorId
            connector_counter = connectors.size
            connector forward streamReq
          }

          else {
            log.warning("All preconfigured connector-servers are taken")
          }

      }
    }

    case connectReq @ ConnectTo (`cmId`, _) =>
      endpoints.get(connectReq.connectorId) match {
        case Some(endpoint) =>
          // forward this request and start building pipelines
          // connector accepts request, start building pipelines from its side
          val connection: Flow[ByteString, ByteString, Future[OutgoingConnection]] = Tcp().outgoingConnection(endpoint.host, endpoint.port)
        case None => log.info("This connector endpoint: {} does not exist", connectReq.connectorId)
      }

    case saveSchema @ SaveSchema(_, _, `cmId`, _) => {
      saveSchema.connectorId match {
        case Some(id) => connectors.get(id) match {
          case Some(connector) =>
            log.info("Saving schema {} and forwarding it to {}", saveSchema.schema_id, saveSchema.connectorId)
            schemas += saveSchema.schema_id -> saveSchema.schema
            connector forward saveSchema
          case None =>
            log.info("The requested connector {} does not exist", saveSchema.connectorId)
            sender() ! ConnectorDoesNotExist
        }
        case None => {
          log.info("Saving schema for the connectorManager {}", cmId)
          schemas += saveSchema.schema_id -> saveSchema.schema
          sender() ! SchemaSaved
        }

      }
    }
      // This is for cluster sharding, don't use it now
    case UpdateEndpoints => {
      for ((connectorId, actorRef) <- connectors) actorRef forward UpdateEndpoints

    }

    case StreamRequestStart(cmId, connectorId) =>
      log.warning("Ignoring request for {}. Connector is responsible for {}", cmId, this.cmId)
    case SaveSchema(_, _, cmId, _) =>
      log.warning("Ignoring request for {}. Connector is responsible for {}", cmId, this.cmId)
  }

}