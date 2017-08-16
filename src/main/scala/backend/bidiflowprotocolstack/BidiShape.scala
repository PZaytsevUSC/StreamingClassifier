package backend.bidiflowprotocolstack

import java.nio.ByteOrder

import scala.concurrent._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Balance, BidiFlow, Broadcast, Flow, Framing, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source, Tcp, ZipWith}
import akka.stream.stage._
import akka.util.ByteString
import backend.dialect.ConnectorDialect
import backend.dialect.ConnectorDialect.{Ping, Pong}

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
/**
  * Created by pzaytsev on 6/10/17.
  */


// In general, when time or rate driven processing stages exhibit strange behavior, one of the first solutions to try should be to decrease the input buffer of the affected elements to 1.


object CodecStage {


  def apply(): BidiFlow[ByteString, ConnectorDialect, ConnectorDialect, ByteString, NotUsed] = {

    BidiFlow.fromFunctions(fromBytes, toBytes)
  }

  def toBytes(msg: ConnectorDialect): ByteString = {
    println("called1")
    implicit val order = ByteOrder.LITTLE_ENDIAN
    msg match {
      case Ping(id) => ByteString("p:" + id)
      case Pong(id) => ByteString("o:" + id)
    }
  }

  def fromBytes(msg: ByteString): ConnectorDialect = {
    println("called2")
    implicit val order = ByteOrder.LITTLE_ENDIAN
    val s = msg.utf8String.trim
    s.charAt(0) match {
      case 'p'     => Ping(s.substring(2).toInt)
      case 'o'     => Pong(s.substring(2).toInt)
      case _ => throw new Exception("Parsing error") // should be handled in streaming way

    }
  }
}

object FramingStage {
  def apply(): BidiFlow[ByteString, ByteString, ByteString, ByteString, NotUsed] = BidiFlow.fromGraph(GraphDSL.create() { b =>
    val delimiter = ByteString("***")
    val in = b.add(Framing.delimiter(delimiter, 256, allowTruncation = false))
    val out =  b.add(Flow[ByteString].map(_ ++ delimiter))
    BidiShape.fromFlows(in, out)
  })
}

