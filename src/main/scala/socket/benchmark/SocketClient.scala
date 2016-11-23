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
    var counter = 0
    val snapshotInterval = 1000
    println(s"Sending $msgCount messages:")
    println(s"${"=" * (msgCount / snapshotInterval)}")
    val (elapsed, _) = measure {
      1 to msgCount foreach { i =>
        writeBlockingMsg(s"$i$msg")
        counter += 1
        if (counter % snapshotInterval == 0) print(".")
      }
    }
    println(s"\n=> Total sent: $msgCount, elapsed $elapsed ms, tps ${msgCount.toDouble / elapsed * 1000}")
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