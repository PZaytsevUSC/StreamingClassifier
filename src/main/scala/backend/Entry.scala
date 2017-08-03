package backend

import akka.actor.ActorSystem
import backend.connectormanager.ConnectorManager
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._
/**
  * Created by pzaytsev on 7/27/17.
  */
object Entry extends App{
  implicit val sys = ActorSystem("backend", ConfigFactory.load("backend.conf"))

  ConnectorManager.start("1")



}
