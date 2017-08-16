package backend.connectormanager

import akka.actor.ActorRef
import akka.stream.scaladsl.Flow
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage._
import akka.util.ByteString
import backend.connectormanager.StreamLinkApi.{CMStreamRef, ConnectorStreamRef, Demand, Payload}
import backend.dialect.ConnectorDialect

import scala.collection.mutable
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

case class NextElement(maybeRef: Option[ActorRef], element: ConnectorDialect)

private class ConnectorEndpointStage(cmRef: ActorRef) extends GraphStage[FlowShape[ConnectorDialect, ConnectorDialect]]{

  val in = Inlet[ConnectorDialect]("ClientBound")
  val out = Outlet[ConnectorDialect]("ConnectorBound")

  override val shape: FlowShape[ConnectorDialect, ConnectorDialect] = FlowShape(in, out)



  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {

    lazy val self = getStageActor(onMessage)
    var queue: mutable.Queue[NextElement] = mutable.Queue()

    override def preStart(): Unit = {
      cmRef ! ConnectorStreamRef(self.ref)

    }

    // if element on input port ready
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        pull(in)
      }
    })

    // if downstream is ready for new elements
    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pushToConnector()
        // pull so far, when performing live linking should be a demand signal
      }
    })
    // a mini actor that deals with linking live streams
    private def onMessage(message: (ActorRef, Any)): Unit = message match {
      case (_, Payload(ref, msg)) =>
        println("hit stage endpoint")
        queue = queue :+ NextElement(Some(ref), msg)
        pushToConnector()
        ref ! Demand(self.ref)
      case (_, CMStreamRef(ref, msg)) =>
        println("CES found where client is")
        ref ! Demand(self.ref)
      case _ => println("CES got something else")

    }


    private def pushToConnector() = {

      if(isAvailable(out) && queue.nonEmpty){
        queue.dequeue() match {
          case (NextElement(ref, elem)) => {
            push(out, elem)
            ref foreach(_ ! Demand(self.ref))
          }
        }
      }

    }
  }


}
