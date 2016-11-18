import java.nio.file.DirectoryStream.Filter
import java.nio.file.{Files, Path, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Framing, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape}
import akka.util.ByteString

import scala.collection.JavaConversions._
import scala.collection.immutable.Seq
import scala.concurrent.Future

object Example_02 extends App {
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val start = System.currentTimeMillis()

  RunnableGraph.fromGraph(
    GraphDSL.create() { implicit b ⇒
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

      val addresses = Flow[Iterable[(String, (String, Path))]].mapAsync(4) {
        case cityDataSet ⇒
          val map: Iterable[Future[Seq[Address]]] = cityDataSet.map {
            case (state, (city, cityDataPath)) ⇒
              val bytesToString = Flow[ByteString]
                .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
                .map(_.utf8String)

              val toEntity = Flow.fromGraph(GraphDSL.create() { implicit b ⇒
                val flow = b.add(Flow[String].map(x ⇒ {
                  val Array(lon, lat, number, street, unit, _, district, _, postcode, _, _) = x.split(",")
                  //println(street)
                  Address(lon, lat, number, street, unit, city, district, state, postcode)
                }))

                FlowShape(flow.in, flow.out)
              })

              FileIO
                .fromPath(cityDataPath)
                .via(bytesToString)
                .drop(1)
                .via(toEntity)
                .runWith(Sink.seq)
          }
          Future.sequence(map).map(_.flatten)
      }

      val countFuture = Source.single(Paths.get("/home/focusj/workspace/datasource/us"))
        .via(citiesData)
        .via(addresses)
        .runFold(0)((r, e) ⇒ r + e.size)

      countFuture.onComplete(println)

      ClosedShape
    }).run()

  println("cost: " + (System.currentTimeMillis() - start))

}
