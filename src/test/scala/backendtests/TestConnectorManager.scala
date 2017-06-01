package backendtests

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

/**
  * Created by pzaytsev on 5/30/17.
  */
class TestConnectorManager extends TestKit(ActorSystem("test_system")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll{

  override  def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }




}
