package socket.benchmark

import java.net.InetSocketAddress

object Main extends App {
  val connection = new SocketClient(new InetSocketAddress("127.0.0.1", 8080), 50000)
  connection.sendAndForgetBlocking("ping")
}
