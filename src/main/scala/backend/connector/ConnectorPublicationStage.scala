package backend.connector

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import backend.dialect.ConnectorDialect
import backend.dialect.ConnectorDialect.{Ping, Pong}

/**
  * Created by pzaytsev on 7/8/17.
  */

/**
  * This stage should somehow consume the source depending on the source type,
  * check for the availablity of data and start publishing data according to spec.
  *
  * Start publishing data according to spec.
  * Produce pong messages if ping is requested.
  */

object ConnectorPublicationStage {
  def apply(): Flow[ConnectorDialect, ConnectorDialect, NotUsed] = Flow.fromGraph(new ConnectorPublicationStage())
}

private class ConnectorPublicationStage extends GraphStage[FlowShape[ConnectorDialect, ConnectorDialect]] {

  val in: Inlet[ConnectorDialect] = Inlet("Incoming")
  val out: Outlet[ConnectorDialect] = Outlet("Outgoing")
  override def shape: FlowShape[ConnectorDialect, ConnectorDialect] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    // should be some handler method to publish but so far just list them.
    // also should be some timer to give actual info on roundtrips like an actual ICMP.
    // Come up with a subscription mechanism for this end.
    // There should be an async boundary between publishing and accessing data from sources.

    setHandler(in, new InHandler {
      override def onPush() = grab(in) match {
        case Ping(id) => push(out, Pong(id))
      }
    })


    setHandler(out, new OutHandler {
      override def onPull() = pull(in)
    })
  }


}
