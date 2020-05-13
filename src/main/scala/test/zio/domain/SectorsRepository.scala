package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.GameTicket
import test.zio.infrastructure.InMemorySectorsRepository
import zio._
import zio.clock.Clock

object SectorsRepository {
  type SectorRepository = Has[Service] with Database with Clock

  trait Service {
    def reserveSeats(noOfSeats: Int, sectorName: String, game: String): ZIO[Database with Clock, String, List[GameTicket]]
  }

  def reserveSeats(noOfSeats: Int, sectorName: String, game: String): ZIO[SectorRepository, String,  List[GameTicket]] =
    ZIO.accessM(_.get.reserveSeats(noOfSeats, sectorName, game))


  val live = ZLayer.succeed[Service] {
    InMemorySectorsRepository()
  }


}
