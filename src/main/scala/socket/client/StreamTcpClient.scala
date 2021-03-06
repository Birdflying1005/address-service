package socket.client

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, Framing, Source, Tcp}
import akka.util.ByteString

object StreamTcpClient extends App {
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()

  val connection = Tcp().outgoingConnection("127.0.0.1", 8080)

  val parse = Flow[String].takeWhile(_ != "q")
    .concat(Source.single("BYE"))
    .map(elem => ByteString(s"$elem\n"))

  val flow = Flow[ByteString]
    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
    .map(_.utf8String)
    .map(text => println("Server: " + text))
    .map(_ => "ping")
    .via(parse)

  connection.join(flow).run()

}
