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
  val servers:List[String] = sys.settings.config.getStringList("connector.servers.enabled").asScala.toList

  val l = List() ++  servers map {id => sys.settings.config.getString(s"connector.servers.$id.host")}

  println(servers)
  println(l)

  // ConnectorManager.start("1")



}
