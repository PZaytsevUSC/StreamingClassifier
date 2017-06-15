package backend.connector

import java.nio.file.{Path, Paths}

import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.FanInShape.Init
import akka.stream._
import akka.stream.scaladsl.{Balance, Broadcast, FileIO, Flow, Framing, GraphDSL, Keep, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip, ZipWith}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.Random

/**
  * Created by pzaytsev on 6/2/17.
  */

// a good shape to use would be the one that separates good streaming data from bad

// also a shape that prioritizes one class over the other



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

    // can be partial graphs
    // should be an api for partial graph shapes adaptation
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


  // using run once in the morning and once in the evening you can materialize stats
  // within some time frame
  def summing_graph(source: Source[String, Future[IOResult]]): RunnableGraph[Future[Int]] = {
    val count_transofmer: Flow[String, Int, NotUsed] = Flow[String].map(_ => 1)

    val sum_sunk: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

    val counter_graph: RunnableGraph[Future[Int]] = datasource_framed("file2.txt").via(count_transofmer).toMat(sum_sunk)(Keep.right)

    counter_graph
  }

}

abstract class ConnectorBaseStreamEndpointStage[T] extends GraphStage[SourceShape[T]]{

  val out: Outlet[T] = Outlet("Abstract Source")
  override val shape: SourceShape[T] = SourceShape(out)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic



}


// only one subscriber -> use fan out to subscribe each subscriber
// part of graphs can be materialized by one actor and run by another actor

object Playground extends App {

  import Helpers.datasource_framed

  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val d = List(1, 2, 3)


}