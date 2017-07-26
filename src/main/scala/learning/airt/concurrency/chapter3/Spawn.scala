package learning.airt.concurrency.chapter3

import com.typesafe.scalalogging.LazyLogging
import resource.managed

import scala.util.Try

object Spawn extends LazyLogging {

  def spawn[A](block: => A): Try[A] = {
    import java.io._
    val actionFile = File.createTempFile(s"spawn-action-${System.currentTimeMillis()}", ".tmp")
    val resultFile = File.createTempFile(s"spawn-result-${System.currentTimeMillis()}", ".tmp")
    actionFile.deleteOnExit()
    resultFile.deleteOnExit()

    managed(new ObjectOutputStream(new FileOutputStream(actionFile))) foreach {
      _ writeObject (() => block)
    }

    import scala.sys.process._
    val command = Seq(
      "java", "-cp",
      classPaths mkString ":",
      SpawnExecutor.className,
      actionFile.getCanonicalPath,
      resultFile.getCanonicalPath
    )

    logger debug s"command:\n${command mkString " "}"

    Try(command.!!) flatMap { output =>
      logger debug s"spawn output: [\n$output\n]"
      managed(new ObjectInputStream(new FileInputStream(resultFile))).
        map(_.readObject().asInstanceOf[Try[A]]).
        tried.flatten
    }
  }

  private def classPaths = {
    val dependenciesClassPath = "target/streams/test/dependencyClasspath/$global/streams/export"
    val dependencies = managed(io.Source fromFile dependenciesClassPath).map(_.getLines().toList).opt.get
    val scv = util.Properties.versionNumberString split "\\." take 2 mkString "."
    val sources = Seq(s"target/scala-$scv/classes", s"target/scala-$scv/test-classes")
    sources ++ dependencies
  }

}

object SpawnExecutor {

  def main(args: Array[String]) {
    import java.io._
    val Array(actionInputPath, resultOutputPath) = args
    for {
      actionInput <- managed(new ObjectInputStream(new FileInputStream(actionInputPath)))
      resultOutput <- managed(new ObjectOutputStream(new FileOutputStream(resultOutputPath)))
    } {
      val action = actionInput.readObject().asInstanceOf[() => Any]
      val result = Try(action())
      resultOutput.writeObject(result)
    }
  }

  def className: String = SpawnExecutor.getClass.getName.stripSuffix("$")

}
