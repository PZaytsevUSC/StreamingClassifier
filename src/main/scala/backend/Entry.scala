package backend

import akka.actor.ActorSystem
import backend.connector.Connector
import backend.connector.Connector.Messages.Startup
import backend.connectormanager.CMMCommands.ConnectTo
import backend.connectormanager.ConnectorManager
import backend.messages.CMMsg.Initialize
import backend.messages.ConnectorMsg.StreamRequestStart
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
/**
  * Created by pzaytsev on 7/27/17.
  */
object Entry{
//  implicit val sys = ActorSystem("backend", ConfigFactory.load("backend.conf"))
//  val con = Connector.start("1", "con1")
//  con ! Startup
//  val cm = ConnectorManager.start("1")
//  cm ! Initialize
//  cm ! StreamRequestStart("1", "con1")
//  cm ! ConnectTo("1", "con1")



}
