package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.Seat
import test.zio.infrastructure.InMemorySectorsRepository
import zio._
import zio.stm.ZSTM

object SectorsRepository {
  type SectorRepository = Has[Service] with Database

  trait Service {
    def reserveSeats(noOfSeats: Int, sectorName: String): ZIO[Database, String, List[Seat]]
    // should be continuous seats
    // should be private, but used to show how to access STM
    def findFreeSeats(noOfSeats: Int, sectorName: String): ZSTM[Database, String, List[Seat]]
  }

  def reserveSeats(noOfSeats: Int, sectorName: String): ZIO[SectorRepository, String,  List[Seat]] =
    ZIO.accessM(_.get.reserveSeats(noOfSeats, sectorName))

  def findFreeSeats(noOfSeats: Int, sectorName: String): ZSTM[SectorRepository, String, List[Seat]] =
    ZSTM.accessM(_.get.findFreeSeats(noOfSeats, sectorName))

  val live = ZLayer.succeed[Service] {
    InMemorySectorsRepository()
  }


}
