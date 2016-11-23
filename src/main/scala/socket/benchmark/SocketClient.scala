package socket.benchmark

import java.io.{OutputStreamWriter, PrintWriter}
import java.net.{InetSocketAddress, Socket}

class SocketClient(val serverAddress: InetSocketAddress, msgCount: Int) {
  val serverSocket = {
    val socket = new Socket()
    socket.setSoTimeout(1000)
    socket.connect(serverAddress)
    socket
  }

  def sendAndForgetBlocking(msg: String) = {
    val (elapsed, _) = measure {
      1 to msgCount foreach { i =>
        writeBlockingMsg(s"$i$msg")
      }
    }
    elapsed
  }

  def close() = serverSocket.close()

  private def writeBlockingMsg(msg: String): Unit = {
    val out = new PrintWriter(new OutputStreamWriter(serverSocket.getOutputStream, "utf-8"), true)
    out.println(msg)
    out.flush()
  }

  private def measure[T](callback: ⇒ T): (Long, T) = {
    val start = System.currentTimeMillis
    val res = callback
    val elapsed = System.currentTimeMillis - start
    (elapsed, res)
  }

}