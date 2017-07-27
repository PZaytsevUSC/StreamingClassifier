package backend.connectormanager

import akka.actor.ActorRef
import akka.stream.scaladsl.Flow
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage._
import akka.util.ByteString
import backend.dialect.ConnectorDialect

/**
  * Created by pzaytsev on 6/19/17.
  */

/**
  *
  * When materialized it should handle the logic of pulling or pushing in elements within
  * CMs JVM.
 */

// Some part of code should handle connector-bound pushing logic
// Some part of code should handle delegator-bound pulling logic

object ConnectorEndpointStage {
  def apply(connectorManagerRef: ActorRef) = Flow.fromGraph(new ConnectorEndpointStage(connectorManagerRef))

}

private class ConnectorEndpointStage(cmRef: ActorRef) extends GraphStage[FlowShape[ConnectorDialect, ConnectorDialect]]{

  val in = Inlet[ConnectorDialect]("ClientBound")
  val out = Outlet[ConnectorDialect]("ConnectorBound")

  override val shape: FlowShape[ConnectorDialect, ConnectorDialect] = FlowShape(in, out)



  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {

    lazy val self = getStageActor(onMessage)

    override def preStart(): Unit = {
      // pass its own reference to connectormanager, connectormanager will forward it to registry
      // registry will forward to delegator
    }

    // if element on input port ready
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        push(out, grab(in))
      }
    })

    // if downstream is ready for new elements
    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
        // pull so far, when performing live linking should be a demand signal
      }
    })
    // a mini actor that deals with linking live streams
    private def onMessage(message: (ActorRef, Any)): Unit = message match {
      case (_, el) => log.warning(s"Unexpected: $el")
    }
  }


}
