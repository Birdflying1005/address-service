import java.nio.file.DirectoryStream.Filter
import java.nio.file.{Files, Path, Paths}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Framing, GraphDSL, Sink}
import akka.stream.{ActorMaterializer, FlowShape}
import akka.util.ByteString

import scala.collection.JavaConversions._

//LON,LAT,NUMBER,STREET,UNIT,CITY,DISTRICT,REGION,POSTCODE,ID,HASH
case class Address(longitude: String, latitude: String, streetNo: String, street: String,
                   unit: String, city: String, distinct: String, region: String, postCode: String)

object Example extends App {
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()

  def getCityAddressData(path: String = "/home/focusj/workspace/datasource/us"): Iterable[(String, Iterable[Path])] = {
    Files.newDirectoryStream(Paths.get(path)).map { statePath ⇒
      val state = statePath.getFileName.toString.toUpperCase()

      val filter = new Filter[Path]() {
        override def accept(path: Path): Boolean = {
          path.getFileName.toString.endsWith(".csv")
        }
      }

      state -> Files.newDirectoryStream(statePath, filter).map(_.getFileName)
    }
  }

  val bytesToString = Flow[ByteString]
    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
    .map(_.utf8String)

  val toEntity = Flow.fromGraph(GraphDSL.create() { implicit b ⇒
    val flow = b.add(Flow[String].map(x ⇒ {
      println(x)
      val Array(lon, lat, number, street, unit, _, district, _, postcode, _, _) = x.split(",")
      Address(lon, lat, number, street, unit, "honolulu", district, "HI", postcode)
    }))

    FlowShape(flow.in, flow.out)
  })

  FileIO
    .fromPath(Paths.get("/home/focusj/workspace/datasource/us/hi/honolulu.csv"))
    .via(bytesToString)
    .drop(1)
    .via(toEntity)
    .runWith(Sink.foreach(println))

}
