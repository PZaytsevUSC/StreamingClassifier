package backend.connectormanager
import akka.actor.{Actor, ActorRef, FSM, IndirectActorProducer, PoisonPill, Props, Stash}
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

/// If it has conenctors of one particular type, it should use routing to load balance

/// Firstly local implementation that clustered implementation

object CMMCommands {
  sealed trait CMMCommand
  case class ConnectTo(ref: ActorRef) extends CMMCommand
  case class SuccessfullyConnected(connection: OutgoingConnection) extends CMMCommand
  case object ConnectionFailed extends CMMCommand
}


object ConnectorManager {
  def props_self(): Props = Props(new ConnectorManager)
}

// Waiting -> Add Connector -> Build pipiline -> Monitor
class ConnectorManager extends Actor with Stash{

  import CMMCommands._
  import context._
  implicit val sys = context.system
  implicit val disp = context.dispatcher
  implicit val log = Logging(sys, this)

  var connector_counter: Int = 0

  var connectors: ListBuffer[ActorRef] = new ListBuffer[ActorRef]()

  // disable consecutive calls to prestart
  override def postRestart(reason: Throwable): Unit = ()

  // prevent from stopping all the children -> should check a stash box on startup
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    postStop()
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
    case Initialize => become(connector_creator)
    case Destroy => context.stop(self)
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
    case Create(host, port) => {
      val endpoint: Endpoint = new Endpoint(host, port)
      val connector = sys.actorOf(props_connector(endpoint), "connector" + connector_counter)
      connector_counter += 1
      connectors += connector
      self ! ConnectTo(connector)
      become(connector_handler)
      // self ! ConnectTo(connector)
    }



    case DestroyConnector(ref: ActorRef) => {
      val connector: ActorRef = connectors.find(x => x == ref).get
      connector ! PoisonPill
    }
  }








}