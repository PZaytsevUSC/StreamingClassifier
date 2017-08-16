package backend.connectorendpointregistry

import akka.actor.{Actor, ActorRef, ActorRefFactory, ActorSystem, Props}
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
object EndpointRegistry {
  private val id = "registry"
  val path = "/user/registry"
  def selection(implicit ctx: ActorRefFactory) = ctx.actorSelection(path)
  def start()(implicit sys: ActorSystem) = sys.actorOf(Props[EndpointRegistry], id)
}

class EndpointRegistry extends Actor {

  /**
    * clients are DelegatorBoundStages
    */
    private var clients: List[CMStreamRef] = List()
    private var connectorEndPointStageRef: Option[ActorRef] = None

  override def receive = {


    case m: CMStreamRef =>
      context.watch(m.ref)
      clients :+=  m
      println("added client")
      connectorEndPointStageRef foreach (_ ! m)

    case ConnectorStreamRef(ref) =>
      println("added servr")
      context.watch(ref)
      connectorEndPointStageRef = Some(ref)
      clients foreach (ref ! _)
    case _ => println("something other")
  }
}
