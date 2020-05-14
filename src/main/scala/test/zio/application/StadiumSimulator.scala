package test.zio.application

import test.zio.domain.Tickets
import test.zio.domain.Tickets.Tickets
import zio.ZIO
import zio.duration._

import scala.util.Random

object StadiumSimulator {

  private val sectorLetters = 'A' to 'U'
  def randomSector = sectorLetters(Random.nextInt(21))

  def ticketsOffice(id: Int, rounds: Int, tickets: Int, game: String): ZIO[Tickets, String, Unit] =
    for {
      f <- ZIO.foreach(1 to rounds)(_ => Tickets.reserveSeats(id, tickets, randomSector.toString, game).fork.delay(80.milliseconds))
      r <- ZIO.foreach(f)(_.join)
      result = r.flatten
      _ <- ZIO.succeed(println(s"TicketDesk id: $id sold: ${result.size}"))
    } yield()


  def soldSummary: ZIO[Tickets, Nothing, Unit] =
    for {
      sold <- Tickets.soldTickets()
      _    <- ZIO.succeed(println(s"Sold total : $sold"))
    } yield ()

}
