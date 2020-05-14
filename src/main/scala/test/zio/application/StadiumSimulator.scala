package test.zio.application

import test.zio.domain.Database.Database
import test.zio.domain.Tickets.TicketService
import test.zio.domain.{Database, Tickets}
import zio.ZIO
import zio.duration._

object StadiumSimulator {

  def ticketsOffice(id: Int, rounds: Int, tickets: Int, game: String): ZIO[TicketService with TicketService, String, Unit] =
    for {
      f <- ZIO.foreach(1 to rounds)(_ => Tickets.reserveSeats(tickets, "A", game).fork.delay(80.milliseconds))
      r <- ZIO.foreach(f)(_.join)
      result = r.flatten
      _ <- ZIO.succeed(println(s"ticketDesk id: $id sold: ${result.size}"))
    } yield()

  def soldSummary: ZIO[Database, Nothing, Unit] =
    for {
      sold <- Database.count()
      _    <- ZIO.succeed(println(s"sold total : $sold"))
    } yield ()

}
