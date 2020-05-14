package test.zio.infrastructure

import test.zio.domain.Sectors
import test.zio.domain.model.SectorWithCapacity
import zio.UIO

case class InMemorySectors() extends Sectors.Service {

  override def sectorsList(): UIO[List[SectorWithCapacity]] = UIO.foreach(('A' to 'Z').toList) { s =>
    UIO.succeed(SectorWithCapacity(s.toString)) }

  override def sectorCapacity(s: Char): UIO[Option[SectorWithCapacity]] =
    sectorsList().map(_.find(_.name == s.toString))
}
