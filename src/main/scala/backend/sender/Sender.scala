package backend.sender

import akka.actor.Actor

/**
  * Created by pzaytsev on 4/9/17.
  */


object Sender{

}

// Subscriber to data streams
class Sender extends Actor{
  implicit val sys = context.system
  implicit val disp = context.dispatcher
  def receive = ???

}
