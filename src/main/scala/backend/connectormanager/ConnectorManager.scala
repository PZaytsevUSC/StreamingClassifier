package backend.connectormanager

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, FSM, IndirectActorProducer, PoisonPill, Props, Stash}
import akka.event.Logging
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import backend.connector.Connector
import backend.connector.Connector.Endpoint
import backend.connector.Connector.props_connector
import backend.messages.CMMsg._

import scala.collection.JavaConverters._
import language.postfixOps
import akka.stream.scaladsl.{Flow, Source, Tcp}
import akka.stream.scaladsl.Tcp.{IncomingConnection, OutgoingConnection, ServerBinding}
import akka.util.ByteString
import backend.bidiflowprotocolstack.{CodecStage, FramingStage}
import backend.connectorendpointregistry.EndpointRegistry
import backend.connectormanager.StreamLinkApi.{CMStreamRef, ConnectorStreamRef}
import backend.messages.ConnectorMsg.{SaveSchema, StreamRequestStart}
import backend.schema.Schema
import com.typesafe.config.ConfigFactory

import scala.util
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

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

  def start(cmId: String, registryRef: ActorSelection)(implicit sys: ActorSystem) = {
    val cfg = sys.settings.config
    val servers:List[String] = sys.settings.config.getStringList("connector.servers.enabled").asScala.toList
    val endpoints = List() ++ servers map {id => Endpoint(sys.settings.config.getString(s"connector.servers.$id.host"),
      sys.settings.config.getString(s"connector.servers.$id.port").toInt) }
    sys.actorOf(props_self(cmId, endpoints, registryRef))
    }


  def props_self(cmId: String, endpoints: List[Endpoint], registryRef: ActorSelection): Props = Props(new ConnectorManager(cmId, endpoints, registryRef))
  }


class ConnectorManager(cmId: String, endpoints: List[Endpoint], registryRef: ActorSelection) extends Actor with Stash with ActorLogging{

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
  private var endpoints_map: Map[String, Endpoint] = Map.empty[String, Endpoint]
  private var connectors_backward: Map[ActorRef, String] = Map.empty[ActorRef, String]
  override def preStart(): Unit = log.info("ConnectorManager {} is up", cmId)
  override def postStop(): Unit = log.info("ConnectorManager {} is down", cmId)

  override def postRestart(reason: Throwable): Unit = ()

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
    case Initialize => log.info("{} connector manager initializing", cmId); sender() ! "Initialized"; become(connector_creator)
    case Destroy => context.stop(self)
    case _ => sender() ! "Non Initialized"
  }


  def connectionFailed: Receive = {
    case _ => unbecome()
  }

  def successfullyConnected: Receive = {
    case ConnectorStreamRef(ref) =>
      println("CM got msg. forwarding to registry now")
      context.watch(ref)
      registryRef ! ConnectorStreamRef(ref)
      // when this actor is created it should know the actor selection(path) of a registry
      // so that when it receives ConnectorStreamRef
      // it can distribute to registry
      // and registry to current clients
    // should be a terminate current connection case too.

  }

  def connector_creator: Receive = {

    case streamReq @ StreamRequestStart(`cmId`, _) => {
      connectors.get(streamReq.connectorId) match {
        case Some(connector) => connector forward streamReq
        case None =>
          log.info("Creating a connector for {}", streamReq.connectorId)

            val endpoint = endpoints.head
            val connector = context.actorOf(props_connector(streamReq.cmId, streamReq.connectorId))
            context.watch(connector)
            connectors += streamReq.connectorId -> connector
            endpoints_map += streamReq.connectorId -> endpoint
            connectors_backward += connector -> streamReq.connectorId
            connector_counter = connectors.size
            connector forward streamReq
      }
    }

    case ConnectorStreamRef(ref) =>
      println("CM got msg. forwarding to registry now")
      context.watch(ref)
      
      registryRef ! ConnectorStreamRef(ref)
    // when this actor is created it should know the actor selection(path) of a registry
    // so that when it receives ConnectorStreamRef
    // it can distribute to registry
    // and registry to current clients
    // should be a terminate current connection case too.


    case connectReq @ ConnectTo (`cmId`, _) =>

      endpoints_map.get(connectReq.connectorId) match {
        case Some(endpoint) =>
          val pipeline = FramingStage() atop CodecStage() join ConnectorEndpointStage(self)
          // async handler to process request further.
          // this thread stays and continues a main work.
          Tcp().outgoingConnection(endpoint.host, endpoint.port) join pipeline run() onComplete{
            case Success(s) =>
              log.info("Successfully connected to {}:{}, handling asynchronously", endpoint.host, endpoint.port)
              become(successfullyConnected)
            case Failure(f) =>
              log.warning("Failed to connect to {}:{}, retreating back to original behavior", endpoint.host, endpoint.port)
              become(connectionFailed)
          }
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