package backend.connector

import java.nio.file.{Path, Paths}

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, Framing, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random

/**
  * Created by pzaytsev on 6/2/17.
  */

object Helpers {

  def redirect(a: String):Int = {
    a.split(",")(1).toInt
  }

  def datasource(f: String): Source[ByteString, Future[IOResult]] = {
    FileIO.fromPath(Paths.get(f))
  }

  def datasource_framed(f: String): Source[String, Future[IOResult]] = {
    datasource(f).via(Framing.delimiter(ByteString("\n"), 256, true)).map(_.utf8String)
  }

  def datasink(f: String): Sink[String, Future[IOResult]] = {
    Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(f)))(Keep.right)
  }

  def split_to_sources_by_category_graph_component(num_of_categories: Int, source: Source[String, Future[IOResult]]): RunnableGraph[NotUsed] = {

    var sinks: Map[Int, Sink[String, Future[IOResult]]] = Map.empty[Int, Sink[String, Future[IOResult]]]

    (0 to num_of_categories)foreach{
      x => sinks += x -> datasink(x + ".txt")
    }

    val graph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val bcast = b.add(Broadcast[String](num_of_categories + 1))
      source ~> bcast.in
      (0 to num_of_categories) foreach {
        x => bcast.out(x) ~> Flow[String].filter(y => x == redirect(y)) ~> sinks.get(x).getOrElse(datasink("nonclassified.txt"))

      }
      ClosedShape
    })

    graph
  }

}

abstract class ConnectorBaseStreamEndpointStage[T] extends GraphStage[SourceShape[T]]{

  val out: Outlet[T] = Outlet("Abstract Source")
  override val shape: SourceShape[T] = SourceShape(out)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic



}




object Playground extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()



}
