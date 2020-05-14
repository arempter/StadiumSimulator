package test.zio.domain

import test.zio.infrastructure.ConsoleRendering
import zio.{Has, ZIO, ZLayer}

import scala.collection.MapView

object Rendering {
  type Rendering = Has[Service]

  trait Service {
    def showSector(sector: String, sold: MapView[Int,List[Int]]): ZIO[Any, Throwable, Unit]
  }

  def showSector(sector: String, sold: MapView[Int,List[Int]]): ZIO[Rendering, Throwable, Unit] =
    ZIO.accessM(_.get.showSector(sector, sold))

  val live = ZLayer.succeed[Service](ConsoleRendering())

}
