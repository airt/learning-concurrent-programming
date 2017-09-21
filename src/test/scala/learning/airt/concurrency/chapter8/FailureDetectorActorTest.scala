package learning.airt.concurrency.chapter8

import akka.actor._
import akka.pattern._
import akka.testkit._
import org.scalatest._

import scala.async.Async._
import scala.concurrent.duration._

class FailureDetectorActorTest
    extends TestKit(ActorSystem())
    with AsyncFreeSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = shutdown(system)

  "Exercises in Chapter 8" - {

    "FailureDetectorActor" - {
      "should work correctly" in async {
        import FailureDetectorActor._
        val probe = TestProbe()
        implicit val sender: ActorRef = probe.ref
        val child = probe childActorOf Props[EmptyActor]
        val detector = system actorOf (FailureDetectorActor.props, "detector")
        detector ! Detect(child, 100.millis, 100.millis)
        await(delay(300.millis))
        probe expectNoMsg ()
        child ! PoisonPill
        await(delay(200.millis))
        probe expectMsg Failed(child)
        await(gracefulStop(detector, 100.millis))
        succeed
      }
    }

  }

}
