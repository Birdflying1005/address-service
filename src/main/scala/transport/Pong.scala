package transport

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class Pong(randomBytes: BitVector) extends MTProto {
  val header = Pong.header
}

object Pong {
  val header = 0x2
}
