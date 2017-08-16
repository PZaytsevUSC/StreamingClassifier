package backendtests

import java.nio.ByteOrder

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{TestKit, TestProbe}
import akka.util.ByteString
import backend.bidiflowprotocolstack.{CodecStage, FramingStage}
import backend.connector.{Connector, ConnectorPublicationStage}
import backend.connectorendpointregistry.EndpointRegistry
import backend.connectormanager.CMMCommands.ConnectTo
import backend.connectormanager.{ConnectorEndpointStage, ConnectorManager}
import backend.connectormanager.ConnectorManager.props_self
import backend.dialect.ConnectorDialect
import backend.dialect.ConnectorDialect.{Ping, Pong}
import backend.messages.CMMsg.Initialize
import backend.messages.ConnectorMsg.StreamRequestStart
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers, WordSpecLike}
import sun.font.TrueTypeFont

import scala.concurrent.Future


/**
  * Created by pzaytsev on 6/19/17.
  */

object HelperFunctions {
  def fromBytes(msg: ByteString): ConnectorDialect = {
    implicit val order = ByteOrder.LITTLE_ENDIAN
    val s = msg.utf8String.trim
    s.charAt(0) match {
      case 'p'     => Ping(s.substring(2).toInt)
      case 'o'     => Pong(s.substring(2).toInt)
      case _ => throw new Exception("Parsing error") // should be handled in streaming way

    }

  }
}

class TestProtocolStack extends TestKit(ActorSystem("test_system", ConfigFactory.load("backend.conf"))) with WordSpecLike with Matchers{

  implicit val order = ByteOrder.LITTLE_ENDIAN
  implicit val mat = ActorMaterializer()

  "A codec stage" must {

    "parse ping and pong messages" in {
      val pings: Source[ByteString, NotUsed] = Source(1 to 10).map(x => ByteString("p:" + x))
      val flow: Flow[ConnectorDialect, ConnectorDialect, NotUsed] = Flow[ConnectorDialect].collect {case Ping(id) => Pong(id)}
      val codecflow = CodecStage() join (flow)
      val probe: Probe[ByteString] = pings.via(codecflow).runWith(TestSink.probe[ByteString])
      val result = probe.request(10).expectNextN(10)
      assert(result.length == 10)
      assert(result.map(x => x.utf8String.trim.charAt(0)).filter(x => x != 'o').isEmpty)
    }

    "return 0 elements if unknown protocol appears" in {
      val unknowns: Source[ByteString, NotUsed] = Source(1 to 10).map(x => ByteString("x:" + x))
      val flow = Flow[ConnectorDialect]
      val codecflow = CodecStage() join (flow)
      val probe: Probe[ByteString] = unknowns.via(codecflow).runWith(TestSink.probe[ByteString])
      val result = probe.request(10).expectNextN(0)
      assert(result.length == 0)
    }
  }

  "A framing stage" must {
    "frame incoming messages and append esc to outgoing messages" in {
      val source_concat: Source[ByteString, NotUsed] = Source(1 to 10)
          .map(ByteString.newBuilder.putInt(_).append(ByteString("***")).result())
          .reduce((x, y) => x ++ y)
      val identity_flow: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString]
      val frameflow = FramingStage() join identity_flow
      assert(source_concat.via(frameflow).runWith(TestSink.probe[ByteString]).request(10).expectNextN(10).length == 10)
    }
  }

  "Codec and Framing stages" must {
    "be stackable" in {
      val source_concat: Source[ByteString, NotUsed] = Source(1 to 10)
        .map(x => ByteString.newBuilder.append(ByteString("p:" + x)).append(ByteString("***")).result())
        .reduce((x, y) => x ++ y)
      val identity_flow: Flow[ConnectorDialect, ConnectorDialect, NotUsed] = Flow[ConnectorDialect]
      val pipeline = FramingStage() atop CodecStage() join identity_flow
      val probe: Probe[ByteString] = source_concat.via(pipeline).runWith(TestSink.probe[ByteString])
      val result = probe.request(10).expectNextN(10)
      assert(result.length == 10)
    }


    "be stackable with ConnectorPublicationStage" in {
      val source_concat: Source[ByteString, NotUsed] = Source(1 to 10)
        .map(x => ByteString.newBuilder.append(ByteString("p:" + x)).append(ByteString("***")).result())
        .reduce((x, y) => x ++ y)
      val cm = ConnectorManager.start("1", EndpointRegistry.selection)
      val pipeline = ConnectorPublicationStage() join (CodecStage().reversed atop FramingStage().reversed)
      val probe: Probe[ByteString] = source_concat.via(pipeline).runWith(TestSink.probe[ByteString])
      val result = probe.request(10).expectNextN(10)
      assert(result.length == 10)

    }

    "work in integration" in {
      val con = Connector.start("1", "con1")
      val probe = TestProbe()
      val cm = ConnectorManager.start("1", EndpointRegistry.selection)
      cm.tell(Initialize, probe.ref)
      cm.tell(StreamRequestStart("1", "con1"), probe.ref)
      cm ! ConnectTo("1", "con1")
    }
  }






}
