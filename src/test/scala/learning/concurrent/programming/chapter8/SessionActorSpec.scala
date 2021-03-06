package learning.concurrent.programming.chapter8

import akka.actor._
import akka.pattern._
import akka.testkit._
import org.scalatest._

import scala.async.Async._
import scala.concurrent.duration._

class SessionActorSpec
    extends TestKit(ActorSystem())
    with ImplicitSender
    with AsyncFreeSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = shutdown(system)

  "Exercises in Chapter 8" - {

    "SessionActor" - {
      "should work correctly" in async {
        import SessionActor._
        val session = system actorOf (SessionActor.props("secret", testActor), "session")
        session ! 0
        expectNoMessage(100.millis)
        session ! StartSession("secret")
        session ! 1
        expectMsg(1)
        session ! StopSession
        session ! 2
        expectNoMessage(100.millis)
        session ! StartSession("incorrect")
        session ! 3
        expectNoMessage(100.millis)
        await(gracefulStop(session, 100.millis))
        succeed
      }
    }

  }

}
