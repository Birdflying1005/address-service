package address

import java.nio.file.DirectoryStream.Filter
import java.nio.file.{Files, Path, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Framing, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape}
import akka.util.ByteString

import scala.collection.JavaConversions._
import scala.util.Try

object Example_04 extends App {
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val citiesData: Flow[Path, Iterable[(String, (String, Path))], NotUsed] = Flow[Path].map(path ⇒
    Files.newDirectoryStream(path).flatMap { statePath ⇒
      val state = statePath.getFileName.toString.toUpperCase()
      val country = path.getFileName.toString.toUpperCase()

      val filter = new Filter[Path]() {
        override def accept(path: Path): Boolean = {
          path.getFileName.toString.endsWith(".csv")
        }
      }
      Files.newDirectoryStream(statePath, filter).map(p ⇒ country -> (state -> p.toAbsolutePath))
    }
  )

  val addresses = Flow[(String, (String, Path))].mapAsync(16) {
    case (state, (city, cityDataPath)) ⇒
      val bytesToString = Flow[ByteString]
        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
        .filter(_.nonEmpty)
        .map(_.utf8String)

      val toEntity = Flow.fromGraph(GraphDSL.create() { implicit b ⇒
        val flow = b.add(
          Flow[String]
            .map(x ⇒ Try {
              val Array(lon, lat, number, street, unit, _, district, _, postcode, _, _) = x.split(",")
              Address(lon, lat, number, street, unit, city, district, state, postcode)
            })
            .filter(_.isSuccess)
            .map(_.get)
        )
        FlowShape(flow.in, flow.out)
      })

      FileIO
        .fromPath(cityDataPath)
        .via(bytesToString)
        .drop(1)
        .via(toEntity)
        .runWith(Sink.seq)
  }

  val t = Source
    .single(Paths.get("/home/focusj/workspace/datasource/us"))
    .via(citiesData)
    .mapConcat(_.toList)
    .via(addresses)
    .runFold(0)((r, e) ⇒ r + e.size)

  val start = System.currentTimeMillis()
  t.onComplete(r ⇒ {
    println(s"there are $r address")
    println(s"total cost is: ${System.currentTimeMillis() - start}")
    system.terminate()
  })
}
