package backend.connectormanager

import akka.stream.{FlowShape, Inlet, Outlet}
import akka.stream.stage.GraphStage
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
abstract class ConnectorEndpointStage() extends GraphStage[FlowShape[ConnectorDialect, ConnectorDialect]]{

  val in = Inlet[ConnectorDialect]("ClientBound")
  val out = Outlet[ConnectorDialect]("ConnectorBound")

  override val shape: FlowShape[ConnectorDialect, ConnectorDialect] = FlowShape(in, out)



}
