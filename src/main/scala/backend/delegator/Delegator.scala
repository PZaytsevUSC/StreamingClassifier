package backend.delegator

import akka.actor.Actor

/**
  * Created by pzaytsev on 5/18/17.
  */

/// Should monitor state of ConnectorManager

/// should be an entry point for a user to create CMs and Connectors on demand

/// CMs can also query data from all the connectors they have at the same time

/// Connector should be connected to an actual device depending on a type by some streaming protocol

/// For example full duplex tcp

/// - Receives a request to stream data from certain endpoint
/// - If manager and connector for it exist, it forwards the request
/// - If not, it creates them
/// - Manager receives a request, if it has a connector, it forwards a message to it
/// - If not it creates connector
/// - Connector recieves a request and ACK to original sender
/// - Original sender learns ActorRef of a connector and is able to query it in the future

class Delegator extends Actor{

  def receive = Actor.emptyBehavior
}
