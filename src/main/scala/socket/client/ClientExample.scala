package socket.client

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString

object Client {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[Client], remote, replies)
}

class Client(remote: InetSocketAddress, listener: ActorRef) extends Actor {

  import context.system

  private var connection: ActorRef = null

  IO(Tcp) ! Connect(remote)

  def active: Receive = {
    case data: ByteString        ⇒
      connection ! Write(data)
    case CommandFailed(w: Write) ⇒
      listener ! "write failed"
    case Received(data)          ⇒
      listener ! data
    case "close"                 ⇒
      connection ! Close
    case _: ConnectionClosed     ⇒
      listener ! "connection closed"
      context stop self

  }

  def receive = {
    case CommandFailed(_: Connect) ⇒
      listener ! "connect failed"
      context stop self

    case c: Connected ⇒
      listener ! c
      connection = sender()
      connection ! Register(self)
      context.become(active)
  }
}

class ClientListener extends Actor {
  override def receive: Receive = {
    case _: CommandFailed    ⇒ println("command send failed")
    case _: ConnectionClosed ⇒ println("connection failed")
    case msg: ByteString     ⇒ println(msg.utf8String)
  }
}

object ClientExample extends App {
  val system = ActorSystem("tcp-server")

  private val listener = system.actorOf(Props[ClientListener], "listener")
  val client = system.actorOf(Client.props(new InetSocketAddress("127.0.0.1", 8080), listener), "serer")

  Thread.sleep(1000)
  client ! ByteString("ping")
  Thread.sleep(1000)
  client ! ByteString("ping")
  Thread.sleep(1000)
  client ! ByteString("ping")
}