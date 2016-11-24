package socket.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, Framing, GraphDSL, Merge, Sink, Source, Tcp}
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

      val toStringFlow = Flow[ByteString]
        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
        .map(_.utf8String)
      val welcomeFlow = Flow[String].merge(Source.single(s"Welcome to: $localAddress, you are: $remoteAddress!"))
      val toByteFlow = Flow[String].map(_ + "\n").map(ByteString(_))

      val completeSink = Sink.onComplete {
        case res ⇒
          res match {
            case Success(_) ⇒
              println("Closing connection")
            case Failure(e) ⇒
              println(s"Closing connection $remoteAddress due to error: $e")
          }
      }

      val messageHandler = Flow.fromGraph(GraphDSL.create() { implicit b ⇒
        import GraphDSL.Implicits._

        val complete = b.add(completeSink)
        val merge = b.add(Merge[ByteString](1))

        val bcast = b.add(Broadcast[ByteString](2))

        bcast.out(0) ~> complete
        bcast.out(1) ~> toStringFlow ~> welcomeFlow ~> toByteFlow ~> merge

        FlowShape(bcast.in, merge.out)
      })

      def balancer[In, Out](worker: Flow[In, Out, Any], workerCount: Int): Flow[In, Out, NotUsed] = {
        import GraphDSL.Implicits._

        Flow.fromGraph(GraphDSL.create() { implicit b =>
          val balancer = b.add(Balance[In](workerCount, waitForAllDownstreams = true))
          val merge = b.add(Merge[Out](workerCount))

          for (_ <- 1 to workerCount) {
            balancer ~> worker.async ~> merge
          }

          FlowShape(balancer.in, merge.out)
        })
      }

      val pongFlow = Flow[ByteString].map(_ => ByteString("pong\n"))
      flow.join(pongFlow).run()
  }

}
