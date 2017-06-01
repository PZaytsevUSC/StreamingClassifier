package backend.connectormanager

import akka.actor.{Actor, ActorLogging, ActorRef, FSM, IndirectActorProducer, PoisonPill, Props, Stash}

import akka.event.Logging
import akka.pattern.ask
import backend.connector.Connector
import backend.connector.Connector.Endpoint
import backend.connector.Connector.props_connector
import backend.messages.CMMsg._

import language.postfixOps
import akka.stream.scaladsl.Tcp
import akka.stream.scaladsl.Tcp.OutgoingConnection

import scala.util
import scala.collection.mutable.ListBuffer
/// Hierarchical structure

/// Should initiate some set of connectors -> monitors connectors like it's children

/// Gives underlying commands to start building and materializing flows

/// Should know the failures and how to handle


//// Dependency Injection

/// Firstly local implementation that clustered implementation

object CMMCommands {
  sealed trait CMMCommand
  case class ConnectTo(ref: ActorRef) extends CMMCommand
  case class ConnectorAdded(connector: String) extends CMMCommand
  case class SuccessfullyConnected(connection: OutgoingConnection) extends CMMCommand
  case object ConnectionFailed extends CMMCommand
}


object ConnectorManager {
  def props_self(): Props = Props(new ConnectorManager)
}

// Waiting -> Add Connector -> Build pipiline -> Monitor
class ConnectorManager extends Actor with Stash with ActorLogging{

  import CMMCommands._
  import context._
  implicit val sys = context.system
  implicit val disp = context.dispatcher
  var connector_counter: Int = 0

  var connectors: ListBuffer[ActorRef] = new ListBuffer[ActorRef]()

  override def preStart(): Unit = log.info("ConnectorManager is up")
  override def postStop(): Unit = log.info("ConnectorManager is down")

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
    case ConnectTo(connector) => {

      /// TCP logic should be here

    // self ! SuccessfullyConnected(c)
    // self ! ConnectionFailed
    }

    case SuccessfullyConnected(connection: OutgoingConnection) => {
      ///
    }

    case ConnectionFailed => {

    }
  }

  def connector_creator: Receive = {
    case Create(requestId, host, port) => {
      val endpoint: Endpoint = new Endpoint(host, port)

      // for cases where there's more than one connector manager this is not an id anymore, change it
      val name = "connector" + connector_counter
      val connector = context.actorOf(props_connector(endpoint), name)

      connector_counter += 1
      connectors += connector
      sender () ! ConnectorAdded(name)
      // should be handled from outside
//      self ! ConnectTo(connector)
//      become(connector_handler)
      // self ! ConnectTo(connector)
    }



    case DestroyConnector(requestId, ref: ActorRef) => {
      val connector: ActorRef = connectors.find(x => x == ref).get
      connector ! PoisonPill
    }
  }








}