package test.zio.application

import test.zio.domain.Tickets.Tickets
import test.zio.domain.{Database, Tickets}
import zio.{IO, ZIO}
import zio.duration._

import scala.util.Random

object StadiumSimulator {

  private val noOfDesks      = 4
  private val random         = Random
  private val game           = "game1"
  private val noOfSaleRounds = 6
  private val maxTickets     = 10

  private def ticketsOffice(id: Int, rounds: Int, tickets: Int, game: String): ZIO[Tickets, String, Unit] =
    for {
      f <- ZIO.forkAll(ZIO.replicate(rounds)(Tickets.reserveSeats(id, tickets, "NotUsed", game)))
      _ <- f.join.map(_.flatten)
    } yield ()

  private def showSalePerDesk(id: Int) =
    for {
      d <- Database.countByDesk(id)
      _ <- ZIO.succeed(println(s"TicketDesk id: $id sold: $d"))
    } yield ()


  private def soldSummary: ZIO[Tickets, Nothing, Unit] =
    for {
      sold <- Tickets.soldTickets()
      _    <- ZIO.succeed(println(s"\n---- Sold Tickets Report -----"))
      _    <- ZIO.succeed(println(s"Sold total : $sold"))
      _    <- ZIO.foreach(1 to noOfDesks)(i=> showSalePerDesk(i))
    } yield ()

  private val handleFailure: Int => ZIO[Any, Nothing, Unit] = id => IO.succeed(println(s"Id: $id Failed to book all")) *> IO.succeed()

  def ticketDeskSimulatorProgram: ZIO[Tickets, String, Unit] =
    (ticketsOffice(1, random.nextInt(noOfSaleRounds), random.nextInt(maxTickets), game).delay(200.milliseconds).orElse(handleFailure(1)) &>
      ticketsOffice(2, random.nextInt(noOfSaleRounds), random.nextInt(maxTickets), game).delay(100.milliseconds).orElse(handleFailure(2)) &>
      ticketsOffice(3, random.nextInt(noOfSaleRounds), random.nextInt(maxTickets), game).delay(50.milliseconds).orElse(handleFailure(3)) &>
      ticketsOffice(4, random.nextInt(noOfSaleRounds), random.nextInt(maxTickets), game).delay(400.milliseconds).orElse(handleFailure(4))) *> soldSummary

}
