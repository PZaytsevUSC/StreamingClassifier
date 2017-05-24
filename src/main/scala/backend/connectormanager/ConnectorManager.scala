package backend.connectormanager
import akka.actor.{Actor, FSM, IndirectActorProducer, Props, Stash}
import backend.connector.Connector
import backend.connector.Connector.Endpoint
import backend.connector.Connector.props_connector
import backend.messages.CMMsg._
import scala.util

/// Hierarchical structure

/// Should initiate some set of connectors -> monitors connectors like it's children

/// Gives underlying commands to start building and materializing flows

/// Should know the failures and how to handle

/// Should use FSM module

//// Dependency Injection

/// Firstly local implementation that clustered implementation

sealed trait State
sealed trait Data


case object Idle extends State
case object CreationPending extends State
case object Created extends State
case object ConnectionPending extends State
case object Connected extends State
case object Disconnected extends State


case object Uninitialized extends Data
case class Connectors(endpoints: List[Endpoint]) extends Data
case class CountOfConnectors(count: Int) extends Data


object ConnectorManager {
  def props_self(): Props = Props(new ConnectorManager)
  def assign_udid(): String = {

  }
}

// Waiting -> Add Connector -> Build pipiline -> Monitor
class ConnectorManager extends FSM[State, Data] with Stash{

  implicit val sys = context.system
  implicit val disp = context.dispatcher
  var connector_counter: Int = 0

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

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Create(host, port), Uninitialized) =>

      val connectors: List[Endpoint] = List()
      val endpoint: Endpoint = new Endpoint(host, port)
      val connector = sys.actorOf(props_connector(endpoint), 'connector1')
      stay using Connectors(List()), CountOfConnectors(1)
  }




}