package socket.benchmark

import java.net.InetSocketAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
  * akka io:
  *   100:   0.14K  37965 msg/sec
  *   1000:  0.52K  43821 msg/sec
  */

object Main extends App {
  import ExecutionContext.Implicits.global

  var connections: List[SocketClient] = Nil

  var c = 100

  def initConnections() = {
    connections = (1 to c).map(_ ⇒ new SocketClient(new InetSocketAddress("127.0.0.1", 8080), 50000)).toList
  }
  def ping() = {
    val tasks = connections.par.map(conn ⇒ Future{ conn.sendAndForgetBlocking("ping") } ).toList
    Future.sequence(tasks).map(_.sum / c)
  }

  var line = StdIn.readLine()
  while(line != null) {
    line match {
      case "init" ⇒ initConnections()
      case "ping" ⇒
        val sendTask = ping()
        sendTask.onComplete {
          case Success(cost) ⇒ println("average cost: " + cost)
          case Failure(ex)   ⇒ println("execute failed cause: " + ex)
        }

      case "exit" ⇒ System.exit(0)
    }

    line = StdIn.readLine()
  }
}
