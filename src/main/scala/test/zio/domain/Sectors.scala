package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.SectorWithCapacity
import test.zio.infrastructure.InMemorySectors
import zio._
import zio.clock.Clock

object Sectors {
  type Sectors = Has[Service] with Database with Clock

  trait Service {
    def sectorsList(): UIO[List[SectorWithCapacity]]
    def sectorCapacity(s: Char): UIO[Option[SectorWithCapacity]]
  }

  def sectorsList(): ZIO[Sectors, Nothing, List[SectorWithCapacity]] = ZIO.accessM(_.get.sectorsList())
  def sectorsCapacity(s: Char): ZIO[Sectors, Nothing, Option[SectorWithCapacity]] = ZIO.accessM(_.get.sectorCapacity(s))

  val live = ZLayer.succeed[Service] {
    InMemorySectors()
  }


}
