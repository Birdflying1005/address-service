package ws

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Broadcast, Flow, Framing, GraphDSL, Sink, Source, Tcp}
import akka.stream.{ActorMaterializer, FlowShape}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ReactivePingPongServer extends App {
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()

  val connections: Source[IncomingConnection, Future[ServerBinding]] = Tcp().bind("127.0.0.1", 8080)

  connections runForeach {
    case (conn@Tcp.IncomingConnection(localAddress, remoteAddress, flow)) ⇒
      println(s"new connection comes in: $remoteAddress")

      val commandParser = Flow[String].takeWhile(_ != "BYE").map(_ + "!")

      val welcome = Source.single(s"Welcome to: $localAddress, you are: $remoteAddress!")

      val messageFlow = Flow[ByteString]
        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
        .map(_.utf8String)
        .via(commandParser)
        .merge(welcome)
        .map(_ + "\n")
        .map(ByteString(_))

      val completeSink = Sink.onComplete {
        case res ⇒
          res match {
            case Success(_) ⇒
              println("Closing connection")
            case Failure(e) ⇒
              println("Closing connection {} due to error: {}", e)
          }
      }

      val messageHandler = Flow.fromGraph(GraphDSL.create() { implicit b ⇒
        import GraphDSL.Implicits._

        val complete = b.add(completeSink)
        val message = b.add(messageFlow)

        val bcast = b.add(Broadcast[ByteString](2))

        bcast ~> complete
        bcast ~> message

        FlowShape(bcast.in, message.out)
      })

      flow.join(messageHandler).run()
  }

}
