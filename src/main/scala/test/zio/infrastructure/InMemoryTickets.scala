package test.zio.infrastructure

import test.zio.domain.Database.Database
import test.zio.domain.Tickets.TicketsEnv
import test.zio.domain.model.{GameTicket, Seat, Sector, Supporter}
import test.zio.domain.{Database, Tickets}
import zio.duration._
import zio.stm.{STM, ZSTM}
import zio.{Schedule, ZIO}

import scala.util.Random

case class InMemoryTickets() extends Tickets.Service {

  private val sectors                 = 'A' to 'U'
  private val rowSize                 = 25
  private val noRows                  = 10
  private val random                  = Random
  private val reserveSeatsRetryPolicy = Schedule.recurs(5) && Schedule.exponential(1.second, 0.2)

  override def ticketsSold(): ZIO[Database, Nothing, Int] = ZIO.accessM[Database](_.get.count())

  private def canBeReserved(newTicket: GameTicket): ZSTM[Database, String, Unit] = {
    val ticketExists: GameTicket => Boolean = gt => gt.seat.seat   == newTicket.seat.seat &&
                                                    gt.seat.row    == newTicket.seat.row &&
                                                    gt.seat.sector == newTicket.seat.sector &&
                                                    gt.game        == newTicket.game

    val outsideCapacity = newTicket.seat.row > noRows || newTicket.seat.seat > rowSize

    for {
     alreadySold <- Database.select(ticketExists)
      _          <- STM.fail(s"$newTicket - Incorrect seat or row!").when(outsideCapacity)
      _          <- STM.fail(s"$newTicket - Seat sold!").when(alreadySold.nonEmpty)
    } yield ()
  }

  private def makeReservation(tickets: Seq[GameTicket]): ZSTM[Database, String, List[GameTicket]] = {
    for {
      _            <- ZSTM.foreach(tickets)(canBeReserved)
      _            <- ZSTM.foreach(tickets)(t=>Database.upsert(t))
    } yield tickets.toList
  }

  override def reserveSeats(deskId: Int, noOfSeats: Int, game: String): ZIO[TicketsEnv, String, List[GameTicket]] =
    ZSTM.atomically(
      for {
        freeSeats <- almostOKSeatSelector(deskId, game, noOfSeats)
        sold      <- makeReservation(freeSeats)
      } yield sold)
    .tapError(e=>ZIO.succeed(println("Error: " + e)))
    .retry(reserveSeatsRetryPolicy)

  override def reserveSeats(seats: Seq[Seat], game: String, supporter: Supporter): ZIO[TicketsEnv, String, List[GameTicket]] = {
    val tickets = seats.map{ seat => GameTicket(game, seat, supporter) }
    ZSTM.atomically(makeReservation(tickets))
  }

  private def generateTicket(game: String, noOfSeats: Int): STM[Nothing, GameTicket] = {
    def swapIfZero(v: Int) = if(v == 0) 1 else v
    def supporter = random.alphanumeric.take(10).mkString
    def row       = { val ch = random.nextInt(noRows);              swapIfZero(ch) }
    def seat      = { val ch = random.nextInt(rowSize - noOfSeats); swapIfZero(ch) }

    STM.succeed(GameTicket(game,
      Seat(Sector(sectors(Random.nextInt(21)).toString), row, seat),
      Supporter(random.nextInt(100), supporter)))
  }

  private def almostOKSeatSelector(deskId: Int, game: String, noOfSeats: Int): ZSTM[Database, String, List[GameTicket]] = {
    for {
      _             <- STM.fail("Incorrect capacity").when(rowSize - noOfSeats < 0)
      randomTicket  <- generateTicket(game, noOfSeats)
      _             <- STM.fail("Selected seat outside capacity").when(randomTicket.seat.seat + noOfSeats > rowSize)
      randomTickets <- {
                          val seats = (randomTicket.seat.seat until randomTicket.seat.seat + noOfSeats).map{ nextSeat =>
                            randomTicket.copy(seat = randomTicket.seat.copy(seat = nextSeat), deskId = deskId) }
                          STM.succeed(seats.toList)
                       }
      _             <- ZSTM.foreach(randomTickets)(canBeReserved)
    } yield randomTickets
  }
}
