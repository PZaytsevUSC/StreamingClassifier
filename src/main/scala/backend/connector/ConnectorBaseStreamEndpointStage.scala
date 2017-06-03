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
abstract class ConnectorBaseStreamEndpointStage[T] extends GraphStage[SourceShape[T]]{

  val out: Outlet[T] = Outlet("Abstract Source")
  override val shape: SourceShape[T] = SourceShape(out)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic

}


object Playground extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  def datasource(f: String): Source[ByteString, Future[IOResult]] = {
    FileIO.fromPath(Paths.get(f))
  }

  def datasink(f: String): Sink[String, Future[IOResult]] = {
    Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(f)))(Keep.right)
  }

  def get_random_class(): String = {
    Math.abs(Random.nextInt()) % 5 toString()
  }

  // Source((1 to 100) map (_ => get_random_class()) ).runForeach(println)


  val ds = datasource("file2.txt").via(Framing.delimiter(ByteString("\n"), 256, true)).map(_.utf8String)
//
//  ds.zipWith(Source((0 to 100) map (_ => get_random_class()) ))((x, y) => s"$x,$y")
//    .map(x => ByteString(x + "\n"))
//    .runWith(FileIO.toPath(Paths.get("file2.txt")))
//    .onComplete(_ => println("complete"))


  // write in different sinks depending on number of classes
  def redirect(a: String):Int = {
    a.split(",")(1).toInt
  }

  var sinks: Map[Int, Sink[String, Future[IOResult]]] = Map.empty[Int, Sink[String, Future[IOResult]]]

  (0 to 4)foreach{
    x => sinks += x -> datasink(x + ".txt")
  }

  // Good code for category stream fan out
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val bcast = b.add(Broadcast[String](4))
    ds ~> bcast.in
    (0 to 3) foreach {
       x => bcast.out(x) ~> Flow[String].filter(y => x == redirect(y)) ~> sinks.get(x).getOrElse(datasink("nonclassified.txt"))

    }
    ClosedShape
  })


  g.run()




}
