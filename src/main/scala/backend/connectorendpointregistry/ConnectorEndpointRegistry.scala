package backend.connectorendpointregistry

import akka.actor.{Actor, ActorRef}
import backend.connectormanager.ConnectorEndpointStage
import backend.connectormanager.StreamLinkApi.{CMStreamRef, ConnectorStreamRef}

/**
  * Created by pzaytsev on 7/3/17.
  */

/**
  * Stream endpoint registry is a centralized repository for stream endpoint locations.
  * It is designed to notify stream endpoints about other stream endpoints' locations.
  * Once the endpoint is registered it's location is broadcasted to other endpoints.
  * Each registry deals with one server - several clients combination
  */

class ConnectorEndpointRegistry extends Actor {
    private var clients: List[CMStreamRef] = List()
    private var connector: Option[ActorRef] = None

  override def receive = {
    case m: CMStreamRef =>
      context.watch(m.ref)
      clients :+=  m
      connector foreach (_ ! m)

    case ConnectorStreamRef(ref) =>
      context.watch(ref)
      connector = Some(ref)
      clients foreach (ref ! _)
  }
}
