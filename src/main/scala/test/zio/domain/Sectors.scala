package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.SectorWithCapacity
import test.zio.infrastructure.InMemorySectorsRepository
import zio._
import zio.clock.Clock

object Sectors {
  type Sectors = Has[Service] with Database with Clock

  trait Service {
    def sectorsList(): UIO[List[Char]]
    def sectorCapacity(s: Char): UIO[SectorWithCapacity]
  }


  val live = ZLayer.succeed[Service] {
    InMemorySectorsRepository()
  }


}
