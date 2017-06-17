package backendtests

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import backend.connectormanager.CMMCommands.{ConnectorDoesNotExist, SchemaSaved}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import backend.connectormanager.ConnectorManager.props_self
import backend.messages.CMMsg.Initialize
import backend.messages.ConnectorMsg.{ConnectorRegistered, SaveSchema, StreamRequestStart}
import backend.schema.{Schema, StructField}
import backend.types.{FloatType, IntType, StringType}

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
      val cm = system.actorOf(props_self("1"))
      cm.tell("Some random message", probe.ref)
      val msg = probe.expectMsg("Non Initialized")
      msg should be ("Non Initialized")
    }

    "Move to initialized state when initialized" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self("1"))
      cm.tell(Initialize, probe.ref)
      val msg = probe.expectMsg("Initialized")
      msg should be ("Initialized")
    }

    "Register a connector" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self("1"))
      cm.tell(Initialize, probe.ref)
      probe.expectMsg("Initialized")

      cm.tell(StreamRequestStart("1", "connector1"), probe.ref)
      probe.expectMsg(ConnectorRegistered)
      val connector1 = probe.lastSender

      cm.tell(StreamRequestStart("1", "connector2"), probe.ref)
      probe.expectMsg(ConnectorRegistered)
      val connector2 = probe.lastSender

      connector1 should !==(connector2)

    }

    "Ignore reqs for a wrong cmId" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self("1"))
      cm.tell(Initialize, probe.ref)
      probe.expectMsg("Initialized")

      cm.tell(StreamRequestStart("random", "connector1"), probe.ref)
      probe.expectNoMsg(500.milliseconds)

    }

    "Return same actor for same ConnectorId" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self("1"))
      cm.tell(Initialize, probe.ref)
      probe.expectMsg("Initialized")

      cm.tell(StreamRequestStart("1", "connector1"), probe.ref)
      probe.expectMsg(ConnectorRegistered)
      val connector1 = probe.lastSender

      cm.tell(StreamRequestStart("1", "connector1"), probe.ref)
      probe.expectMsg(ConnectorRegistered)
    }

    "Save schema if ConnectorId is not provided" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self("1"))
      cm.tell(Initialize, probe.ref)
      probe.expectMsg("Initialized")

      val schema = Schema(Array(
        StructField("name", StringType, false),
        StructField("age", IntType, false),
        StructField("salary", FloatType, false)))

      cm.tell(SaveSchema(123, schema, "1", None), probe.ref)
      probe.expectMsg(SchemaSaved)
    }

    "Save schema and forward the schema to a connector if ConnectorId is provided" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self("1"))
      cm.tell(Initialize, probe.ref)
      probe.expectMsg("Initialized")

      val schema = Schema(Array(
        StructField("name", StringType, false),
        StructField("age", IntType, false),
        StructField("salary", FloatType, false)))

      cm.tell(StreamRequestStart("1", "connector1"), probe.ref)
      probe.expectMsg(ConnectorRegistered)
      cm.tell(SaveSchema(123, schema, "1", Some("connector1")), probe.ref)
      probe.expectMsg(SchemaSaved)
    }

    "Report that connectorId does not exist if ConnectorId is not found" in {
      val probe = TestProbe()
      val cm = system.actorOf(props_self("1"))
      cm.tell(Initialize, probe.ref)
      probe.expectMsg("Initialized")
      val schema = Schema(Array(
        StructField("name", StringType, false),
        StructField("age", IntType, false),
        StructField("salary", FloatType, false)))
      cm.tell(SaveSchema(123, schema, "1", Some("connector1")), probe.ref)
      probe.expectMsg(ConnectorDoesNotExist)

    }



  }


}
