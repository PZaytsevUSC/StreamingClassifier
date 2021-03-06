package backendtests

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import backend.connector.Connector.props_connector
import backend.messages.ConnectorMsg.{ConnectorRegistered, StreamRequestStart}
import com.typesafe.config.ConfigFactory
/**
  * Created by pzaytsev on 6/1/17.
  */
class TestConnector extends TestKit(ActorSystem("test_system", ConfigFactory.load("backend.conf"))) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll{

  override  def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Connector actor" must {
    "Respond with a connector registered message when registered with proper ids" in {
      val probe = TestProbe()
      val connector = system.actorOf(props_connector("123", "456"))
      connector.tell(StreamRequestStart("123", "456"), probe.ref)
      val msg = probe.expectMsg(ConnectorRegistered)
      probe.lastSender should === (connector)

    }

    "Ignoring wrong registration messages when either cmId or connector id are wrong" in {
      val probe = TestProbe()
      val connector = system.actorOf(props_connector("123", "456"))
      connector.tell(StreamRequestStart("wrongcmid", "456"), probe.ref)
      probe.expectNoMsg(500.milliseconds)
      connector.tell(StreamRequestStart("123", "wrongid"), probe.ref)
      probe.expectNoMsg(500.milliseconds)

    }
  }
}
