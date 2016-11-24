package transport

import scodec.bits.BitVector

@SerialVersionUID(1L)
final case class MTPackage(authId: Long, sessionId: Long, messageBytes: BitVector) extends MTProto {
  val header = MTPackage.header
}

object MTPackage {
  val header = 0x0
}
