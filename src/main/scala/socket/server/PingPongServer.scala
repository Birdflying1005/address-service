package socket.server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString


class PingPongMsgHandler extends Actor {

  import Tcp._

  def receive = {
    case Received(data) ⇒ sender() ! Write(ByteString("Pong"))
    case PeerClosed     ⇒ context stop self
  }
}

class PingPongServer extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8080))

  def receive = {
    case Bound(localAddress)      ⇒ println("server is start up ...")
    case CommandFailed(_: Bind)   ⇒ context stop self
    case Connected(remote, local) ⇒
      val handler = context.actorOf(Props[PingPongMsgHandler])
      sender() ! Register(handler)
  }
}

object PingPongServer extends App {

  val system = ActorSystem("tcp-server")

  system.actorOf(Props[PingPongServer], "serer")

}
