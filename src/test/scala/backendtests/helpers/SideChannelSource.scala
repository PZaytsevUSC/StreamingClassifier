package backendtests.helpers

import java.nio.ByteOrder

import akka.NotUsed
import akka.actor.{ActorRef, ActorSelection, ActorSystem}
import akka.stream.scaladsl.{Flow, Source}
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, StageLogging}
import akka.util.ByteString
import backend.bidiflowprotocolstack.{CodecStage, FramingStage}
import backend.connector.Connector
import backend.connector.Connector.Messages.Startup
import backend.connectorendpointregistry.EndpointRegistry
import backend.connectormanager.CMMCommands.ConnectTo
import backend.connectormanager.{ConnectorEndpointStage, ConnectorManager}
import backend.connectormanager.StreamLinkApi.{CMStreamRef, Demand, Payload}
import backend.dialect.ConnectorDialect
import backend.dialect.ConnectorDialect.Ping
import backend.messages.CMMsg.Initialize
import backend.messages.ConnectorMsg.StreamRequestStart
import com.typesafe.config.ConfigFactory

/**
  * Created by pzaytsev on 8/6/17.
  */
object SideChannelSource {
  def apply(ref: ActorSelection) = Source.fromGraph(new SideChannelSource(ref))
}

// A test source with internal actor to test connector and connectormanager infr-re.
// The stage
class SideChannelSource(ref: ActorSelection) extends GraphStage[SourceShape[ByteString]] {



  implicit val order = ByteOrder.LITTLE_ENDIAN
  private var demand = 0
  val out = Outlet[ByteString]("Output")
  // a continuous source of bytestring data
  var data = (1 to 3).map(ByteString.newBuilder.putInt(_).append(ByteString("***")).result())
    .reduce((x, y) => x ++ y)

  var cesRef: Option[ActorRef] = None
  var endPointRef: Option[ActorSelection] = Some(ref)

  override val shape: SourceShape[ByteString] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {

    lazy val self = getStageActor(onMessage)
    override def preStart(): Unit = {
      endPointRef.foreach(_ ! CMStreamRef(self.ref, Ping(1)))
    }

    // needs a buffer?
    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        println("outhandler length " + data.length)
        sendToConnectorEndpoint()
      }
    })

    private def sendToConnectorEndpoint(): Unit = {
      if (demand > 0) {
        if (data.length > 0){
          val head = data.head
          data = data.tail
          println("Data length: " + data.length)
          println("HEAD: " + head)
          cesRef foreach (_ ! Payload(self.ref, Ping(head)))

        }
        else{
          complete(out)
        }

      }
    }
    private def onMessage(message: (ActorRef, Any)): Unit = message match {
      case (_, Demand(ref)) => {
        if(cesRef.isEmpty){
          self.watch(ref)
          cesRef = Some(ref)
        }
        demand = 1
        sendToConnectorEndpoint()

      }
    }
  }
}


// object test_this extends App {
//  implicit val sys = ActorSystem("backend", ConfigFactory.load("backend.conf"))
//  implicit val materializer = ActorMaterializer()
//
//  EndpointRegistry.start()
//
//
//  val s: Source[ByteString, NotUsed] = SideChannelSource(EndpointRegistry.selection)
//  val con = Connector.start("1", "con1")
//  con ! Startup
//
//  val cm = ConnectorManager.start("1", EndpointRegistry.selection)
//  cm ! Initialize
//  cm ! StreamRequestStart("1", "con1")
//  cm ! ConnectTo("1", "con1")
  // val ces = ConnectorEndpointStage(cm)

  // val pipeline = FramingStage() atop CodecStage() join ces

  // lets materialize separately

  // s.runForeach(println(_))
  // val g = s.via(pipeline).runForeach(println(_))

// }
