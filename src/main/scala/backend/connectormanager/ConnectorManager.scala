package backend.connectormanager
import akka.actor.{Actor, IndirectActorProducer, Props, Stash}
import backend.connector.Connector
import backend.connector.Connector.Endpoint
/// Hierarchical structure

/// Should initiate some set of connectors

/// Should know the failures and how to handle

/// Shoud use FSM module

//// Dependency Injection


object ConnectorManager {
  def props_self(): Props = Props(new ConnectorManager)
  def props_connector(endpoints: List[Endpoint]): Props = Props(new Connector(endpoints))
}
class ConnectorManager extends Actor with Stash{

  implicit val sys = context.system
  implicit val disp = context.dispatcher

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

  def receive = ???
}