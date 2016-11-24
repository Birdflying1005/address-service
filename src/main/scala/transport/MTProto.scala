package transport

trait MTProto extends Product with Serializable {
  val header: Int
}
