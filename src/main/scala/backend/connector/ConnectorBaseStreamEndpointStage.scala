package backend.connector

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic}

/**
  * Created by pzaytsev on 6/2/17.
  */
abstract class ConnectorBaseStreamEndpointStage[T] extends GraphStage[SourceShape[T]]{

  val out: Outlet[T] = Outlet("Abstract Source")
  override val shape: SourceShape[T] = SourceShape(T)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic

}
