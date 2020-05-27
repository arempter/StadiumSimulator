package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.{GameTicket, Seat, Supporter}
import test.zio.infrastructure.InMemoryTickets
import zio.clock.Clock
import zio.{Has, ZIO, ZLayer}

object Tickets {
  type Tickets = Has[Service]
  type TicketsEnv = Database with Clock

  trait Service {
    def reserveSeats(deskId: Int, noOfSeats: Int, game: String): ZIO[TicketsEnv, String, List[GameTicket]]
    def reserveSeats(seats: Seq[Seat], game: String, supporter: Supporter): ZIO[TicketsEnv, String, List[GameTicket]]
    def ticketsSold(): ZIO[Database, Nothing, Int]
  }

  def reserveSeats(deskId: Int, noOfSeats: Int, game: String): ZIO[Tickets with TicketsEnv, String,  List[GameTicket]] =
    ZIO.accessM(_.get.reserveSeats(deskId, noOfSeats, game))

  def reserveSeats(seats: Seq[Seat], game: String, supporter: Supporter): ZIO[Tickets with TicketsEnv, String,  List[GameTicket]] =
    ZIO.accessM(_.get.reserveSeats(seats, game, supporter))

  def ticketsSold(): ZIO[Tickets with Database, Nothing, Int] = ZIO.accessM(_.get.ticketsSold())

  val live = ZLayer.succeed[Service](InMemoryTickets())

}
