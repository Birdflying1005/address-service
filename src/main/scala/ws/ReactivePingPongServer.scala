package ws

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Flow, Framing, Source, Tcp}
import akka.util.ByteString

import scala.concurrent.Future

object ReactivePingPongServer extends App {
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()

  val connections: Source[IncomingConnection, Future[ServerBinding]] = Tcp().bind("127.0.0.1", 8080)
  connections runForeach { connection =>
    val commandParser = Flow[String].takeWhile(_ != "BYE").map(_ + "!")

    import connection._
    val welcome = Source.single(s"Welcome to: $localAddress, you are: $remoteAddress!")

    val serverLogic = Flow[ByteString]
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
      .map(_.utf8String)
      .via(commandParser)
      .merge(welcome)
      .map(_ + "\n")
      .map(ByteString(_))

    connection.handleWith(serverLogic)

  }
}
