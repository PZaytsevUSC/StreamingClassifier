package backend.connectormanager

import akka.actor.ActorRef
import backend.dialect.ConnectorDialect
import backend.messages.ConnectorMsg

/**
  * Created by pzaytsev on 7/3/17.
  */

// For out of JVM boundary stream communication, streams should be linked via enclosing them in actor messages
object StreamLinkApi {

  case class DelegatorStreamRef(ref: ActorRef)

  case class CMStreamRef(ref: ActorRef, msg: ConnectorDialect)

  case class ConnectorStreamRef(ref: ActorRef)

  case class Demand(sender: ActorRef)

  case class Payload(sender: ActorRef, msg: ConnectorDialect)
}
