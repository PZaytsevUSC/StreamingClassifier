package backend.messages

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
  case object Destroy extends CMMsg

}
