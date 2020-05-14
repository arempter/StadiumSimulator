package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.GameTicket
import test.zio.infrastructure.InMemoryTicketService
import zio.clock.Clock
import zio.{Has, ZIO, ZLayer}

object Tickets {
  type TicketService = Has[Service] with Database with Clock

  // capacity - number of ticket desks

  trait Service {
    def reserveSeats(noOfSeats: Int, sectorName: String, game: String): ZIO[TicketService, String, List[GameTicket]]
    def soldTickets(): ZIO[Database, Nothing, Int]
  }

  def reserveSeats(noOfSeats: Int, sectorName: String, game: String): ZIO[TicketService, String,  List[GameTicket]] =
    ZIO.accessM(_.get.reserveSeats(noOfSeats, sectorName, game))

  def soldTickets(): ZIO[TicketService, Nothing, Int] = ZIO.accessM(_.get.soldTickets())

  val live = ZLayer.succeed[Service](InMemoryTicketService())

}
