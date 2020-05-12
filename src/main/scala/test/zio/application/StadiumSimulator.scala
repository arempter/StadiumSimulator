package test.zio.application

import test.zio.domain.Database.Database
import test.zio.domain.{Database, SectorsRepository}
import test.zio.domain.SectorsRepository.SectorRepository
import test.zio.domain.Tickets.TicketService
import zio.ZIO
import zio.duration._

object StadiumSimulator {

  def ticketService(id: Int, rounds: Int, tickets: Int): ZIO[SectorRepository with TicketService, String, Unit] =
    for {
      f <- ZIO.foreach(1 to rounds)(_ => SectorsRepository.reserveSeats(tickets, "A").fork.delay(80.milliseconds))
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
