package backendtests

import java.nio.ByteOrder

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import akka.util.ByteString
import backend.bidiflowprotocolstack.{CodecStage, FramingStage}
import backend.dialect.ConnectorDialect
import backend.dialect.ConnectorDialect.{Ping, Pong}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers, WordSpecLike}
import sun.font.TrueTypeFont

import scala.concurrent.Future


/**
  * Created by pzaytsev on 6/19/17.
  */
class TestProtocolStack extends TestKit(ActorSystem("test_system", ConfigFactory.load("backend.conf"))) with WordSpecLike with Matchers{

  implicit val order = ByteOrder.LITTLE_ENDIAN
  implicit val mat = ActorMaterializer()
  "A codec stage" must {

    "parse ping and pong messages" in {
      val pings: Source[ByteString, NotUsed] = Source(1 to 10).map(ByteString.newBuilder.putByte(1).putInt(_).result())
      val flow: Flow[ConnectorDialect, ConnectorDialect, NotUsed] = Flow[ConnectorDialect].collect {case Ping(id) => Pong(id)}
      val codecflow = CodecStage() join (flow)
      val probe: Probe[ByteString] = pings.via(codecflow).runWith(TestSink.probe[ByteString])
      val result = probe.request(10).expectNextN(10)
      assert(result.length == 10)
      assert(result.map(b => b.iterator.getByte).filter(x => x != 2).isEmpty)
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


}
