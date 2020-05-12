package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.{GameTicket, Seat}
import test.zio.infrastructure.InMemorySectorsRepository
import zio._
import zio.clock.Clock
import zio.stm.ZSTM

object SectorsRepository {
  type SectorRepository = Has[Service] with Database with Clock

  trait Service {
    def reserveSeats(noOfSeats: Int, sectorName: String): ZIO[Database with Clock, String, List[GameTicket]]
    // should be continuous seats
    // should be private, but used to show how to access STM
    def findFreeSeats(noOfSeats: Int, sectorName: String): ZSTM[Database, String, GameTicket]
  }

  def reserveSeats(noOfSeats: Int, sectorName: String): ZIO[SectorRepository, String,  List[GameTicket]] =
    ZIO.accessM(_.get.reserveSeats(noOfSeats, sectorName))

  def findFreeSeats(noOfSeats: Int, sectorName: String): ZSTM[SectorRepository, String, GameTicket] =
    ZSTM.accessM(_.get.findFreeSeats(noOfSeats, sectorName))

  val live = ZLayer.succeed[Service] {
    InMemorySectorsRepository()
  }


}
