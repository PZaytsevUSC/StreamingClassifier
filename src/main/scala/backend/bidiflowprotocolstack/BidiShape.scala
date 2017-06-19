package backend.bidiflowprotocolstack

import java.nio.ByteOrder

import scala.concurrent._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Balance, BidiFlow, Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source, Tcp, ZipWith}
import akka.stream.stage._
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
/**
  * Created by pzaytsev on 6/10/17.
  */


// In general, when time or rate driven processing stages exhibit strange behavior, one of the first solutions to try should be to decrease the input buffer of the affected elements to 1.


object bidiFlow extends App{

  implicit val sys = ActorSystem("systemtest")
  implicit val materializer = ActorMaterializer()



}


