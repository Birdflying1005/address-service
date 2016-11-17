import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}

import scala.collection.JavaConversions._

object Example extends App {
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

//  val t = FileIO.fromPath(Paths.get("/home/focusj/WorkSpace/Scala/address-service"))
  Files.newDirectoryStream(Paths.get("/home/focusj/WorkSpace/DataSource/us")).foreach(println)

}
