package ws

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString


class SimplisticHandler extends Actor {

  import Tcp._

  def receive = {
    case Received(data) ⇒
      println(s"server received msg: ${data.utf8String}")
      sender() ! Write(ByteString("Pong"))
    case PeerClosed     ⇒ context stop self
  }
}

class Server extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8080))

  def receive = {
    case b@Bound(localAddress) ⇒
      println("server is start up ...")

    case CommandFailed(_: Bind) ⇒ context stop self

    case c@Connected(remote, local) ⇒
      val handler = context.actorOf(Props[SimplisticHandler])
      sender() ! Register(handler)
  }
}

object EchoServer extends App {

  val system = ActorSystem("tcp-server")

  system.actorOf(Props[Server], "serer")

}
