package transport

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class Ping(randomBytes: BitVector) extends MTProto {
  val header = Ping.header
}

object Ping {
  val header = 0x1
}
