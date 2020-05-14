package test.zio.infrastructure

import test.zio.domain.Sectors
import test.zio.domain.model.SectorWithCapacity
import zio.UIO

case class InMemorySectorsRepository() extends Sectors.Service {

  // todo: capacity per Sector && some validation
  val rowSize = 25
  val noRows = 10

  override def sectorsList(): UIO[List[Char]] = ???

  override def sectorCapacity(s: Char): UIO[SectorWithCapacity] = ???
}
