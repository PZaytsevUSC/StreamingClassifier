package backend.connectormanager

import akka.actor.ActorRef
import akka.stream.scaladsl.Flow
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage._
import backend.connectormanager.StreamLinkApi.Demand
import backend.dialect.ConnectorDialect

import scala.collection.mutable

/**
  * Created by pzaytsev on 8/15/17.
  */

// A client bound stage

// should push, pull, have some sort of buffer
// send from this buffer async to connectorEndpoint stage


// !!!
// pulls only from its in port from client
// sends to the end of the stream only by means of sending messages
// pushes back to client for output port
// should be some mechanism that corresponds request with response


// Exactly same logic for CES
object DelegatorEndpointStage{
  def apply(ref: ActorRef) = Flow.fromGraph(new DelegatorEndpointStage(ref))
}
class DelegatorEndpointStage(ref: ActorRef) extends GraphStage[FlowShape[ConnectorDialect, ConnectorDialect]] {
  private var demand = 0

  val in = Inlet[ConnectorDialect]("in")
  val out = Outlet[ConnectorDialect]("out")
  override val shape = FlowShape(in, out)
  var cesRef: Option[ActorRef] = None

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) with StageLogging {
    lazy val self = getStageActor(onMessage)
    var queue: mutable.Queue[NextElement] = mutable.Queue()

    def forwardToConnectorEndPoint(): Unit = ???

    setHandler(in, new InHandler {
      override def onPush(): Unit = forwardToConnectorEndPoint()
    })

    setHandler(out, new OutHandler {
      override def onPull() = ???
    })
    def takeStreamHead(): Option[ConnectorDialect] = {
      if (isAvailable(out)) {
        val next = grab(in)
        pull(in)
        Some(next)
      }
      else
        None
    }


    private def onMessage(message: (ActorRef, Any)): Unit = message match {
      case (_, Demand(ref)) => {
        if (cesRef.isEmpty) {
          self.watch(ref)
          cesRef = Some(ref)
        }
        demand = 1
        forwardToConnectorEndPoint()

      }
    }
  }
}