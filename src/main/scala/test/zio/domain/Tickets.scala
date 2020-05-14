package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.{GameTicket, Seat, Supporter}
import test.zio.infrastructure.InMemoryTickets
import zio.clock.Clock
import zio.{Has, ZIO, ZLayer}

object Tickets {
  type Tickets = Has[Service] with Database with Clock

  // capacity - number of ticket desks
  trait Service {
    def reserveSeats(noOfSeats: Int, sectorName: String, game: String): ZIO[Tickets, String, List[GameTicket]]
    def reserveSeats(seats: Seq[Seat], game: String, supporter: Supporter): ZIO[Tickets, String, List[GameTicket]]
    def soldTickets(): ZIO[Database, Nothing, Int]
  }

  def reserveSeats(noOfSeats: Int, sectorName: String, game: String): ZIO[Tickets, String,  List[GameTicket]] =
    ZIO.accessM(_.get.reserveSeats(noOfSeats, sectorName, game))

  def reserveSeats(seats: Seq[Seat], game: String, supporter: Supporter): ZIO[Tickets, String,  List[GameTicket]] =
    ZIO.accessM(_.get.reserveSeats(seats, game, supporter))

  def soldTickets(): ZIO[Tickets, Nothing, Int] = ZIO.accessM(_.get.soldTickets())

  val live = ZLayer.succeed[Service](InMemoryTickets())

}
