package backend.messages

import akka.actor.ActorRef
import akka.stream.scaladsl.Tcp
import akka.stream.scaladsl.Tcp.OutgoingConnection
/**
  * Created by pzaytsev on 4/9/17.
  */
trait SenderMsg

object SenderMsg{

  trait SenderToConnector extends SenderMsg
  trait ConnectorToSender extends SenderMsg
  case object Connect extends SenderToConnector
  case object TearDown extends SenderToConnector
}

trait CMMsg

object CMMsg {


  case class Create(host: String, port: Int) extends CMMsg
  case class DestroyConnector(ref:ActorRef) extends CMMsg
  case object Destroy extends CMMsg
  case object Initialize extends CMMsg

}
