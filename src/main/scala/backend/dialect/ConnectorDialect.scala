package backend.dialect

/**
  * Created by pzaytsev on 6/18/17.
  */
trait ConnectorDialect


// should have data request for schema -> data response for schema

object ConnectorDialect {
  trait ServerToClient extends ConnectorDialect
  trait ClientToServer extends ConnectorDialect
  case class Ping(id: Int) extends ClientToServer
  case class Pong(id: Int) extends ServerToClient
}
