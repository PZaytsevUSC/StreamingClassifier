/**
  * Created by pzaytsev on 3/24/17.
  */
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

class MyActor extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "test" => log.info("received test")
    case _      => log.info("received unknown message")
  }
}

object Test  extends App{
  val system = ActorSystem("mySystem")
  val myActor = system.actorOf(Props[MyActor], "myactor2")
  myActor ! "test"
}
