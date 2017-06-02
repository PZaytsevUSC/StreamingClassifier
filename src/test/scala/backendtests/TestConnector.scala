package backendtests

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import backend.connector.Connector.props_connector
import backend.connectormanager.CMMCommands.ConnectorAdded
import backend.messages.ConnectorMsg.{ConnectorRegistered, StreamRequestStart}
/**
  * Created by pzaytsev on 6/1/17.
  */
class TestConnector extends TestKit(ActorSystem("test_system")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll{

  override  def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Connector actor" must {
    "Respond with a connector registered message when registered with proper ids" in {
      val probe = TestProbe()
      val connector = system.actorOf(props_connector("123", "456", None))
      connector.tell(StreamRequestStart("123", "456"), probe.ref)
      val msg = probe.expectMsg(ConnectorRegistered)
      probe.lastSender should === (connector)

    }
  }
}
