package backend.streamingclassifier

import akka.actor.{Actor, ActorLogging, Props}

/**
  * Created by pzaytsev on 5/31/17.
  */

// application level actor
object StreamingClassifier {
  def props(): Props = Props(new StreamingClassifier)

}
class StreamingClassifier extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("SC strarted")
  override def postStop(): Unit = log.info("SC stopped")

  override def receive = Actor.emptyBehavior
}
