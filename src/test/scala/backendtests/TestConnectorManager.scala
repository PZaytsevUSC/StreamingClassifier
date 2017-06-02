package backendtests

import akka.actor.ActorSystem

import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import backend.connectormanager.ConnectorManager.props_self
import backend.connectormanager.CMMCommands.ConnectorAdded
import backend.messages.CMMsg.{Initialize, Create}

/**
  * Created by pzaytsev on 5/30/17.
  */
class TestConnectorManager extends TestKit(ActorSystem("test_system")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll{

  override  def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }


  "A ConnectorManager actor" must {
    "Respond with non-initialized when in non-initialzed state" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self())
      cm.tell("Some random message", probe.ref)
      val msg = probe.expectMsg("Non Initialized")
      msg should be ("Non Initialized")
    }

    "Move to initialized state when initialized" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self())
      cm.tell(Initialize, probe.ref)
      val msg = probe.expectMsg("Initialized")
      msg should be ("Initialized")
    }

  }


}
