package transport

sealed trait MTTransport

@SerialVersionUID(1L)
case class ProtoPackage(m: MTProto) extends MTTransport

@SerialVersionUID(1L)
case object SilentClose extends MTTransport
