package test.zio.application

import java.util.concurrent.TimeUnit

import test.zio.domain.Database.Database
import test.zio.domain.Tickets.{Tickets, TicketsEnv}
import test.zio.domain.{Database, Tickets}
import zio.duration._
import zio.{IO, ZIO}

import scala.util.Random

object StadiumSimulator {

  private val noOfDesks      = 4
  private val random         = Random
  private val game           = "game1"
  private val noOfSaleRounds = 4
  private val maxTickets     = 6

  private def swapIfZero(v: Int) = if(v == 0) 1 else v
  private def randomDuration = Duration.apply(random.nextInt(300), TimeUnit.MILLISECONDS)

  private def ticketsOffice(id: Int,
                            rounds: Int = swapIfZero(random.nextInt(noOfSaleRounds)),
                            tickets: Int = swapIfZero(random.nextInt(maxTickets)),
                            game: String = game
                           ): ZIO[Tickets with TicketsEnv,  String, Unit] =
    for {
      _ <- ZIO.succeed(println(s"Running TicketDesk Id: $id inQueue: $rounds tickets: $tickets"))
      f <- ZIO.forkAll(ZIO.replicate(rounds)(Tickets.reserveSeats(id, tickets, game).delay(randomDuration)))
      _ <- f.join.map(_.flatten)
    } yield ()

  private def showSalePerDesk(id: Int) =
    for {
      d <- Database.countByDesk(id)
      _ <- ZIO.succeed(println(s"TicketDesk Id: $id sold: $d"))
    } yield ()

  private def showSalePerSector() =
    for {
      s <- Database.countBySector()
      _ <- ZIO.succeed(println(s"\nIn sector sold: $s"))
    } yield ()

  private def soldSummary: ZIO[Tickets with Database, Nothing, Unit] =
    for {
      sold <- Tickets.ticketsSold()
      _    <- ZIO.succeed(println(s"\n---- Sold Tickets Report -----"))
      _    <- ZIO.succeed(println(s"\nSold total : $sold"))
      _    <- ZIO.foreach(1 to noOfDesks)(i=> showSalePerDesk(i))
    } yield ()

  private val handleFailure: Int => ZIO[Any, Nothing, Unit] = id => IO.succeed(println(s"Id: $id Failed to book all")) *> IO.succeed()

  def ticketDeskSimulatorProgram: ZIO[Tickets with TicketsEnv, String, Unit] =
    (ticketsOffice(1).orElse(handleFailure(1)) &>
      ticketsOffice(2).orElse(handleFailure(2)) &>
      ticketsOffice(3).orElse(handleFailure(3)) &>
      ticketsOffice(4).orElse(handleFailure(4))) *> (soldSummary *> showSalePerSector())
}
