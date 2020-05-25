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

  private val rowSize                 = 25
  private val noRows                  = 10
  private val random                  = Random
  private val reserveSeatsRetryPolicy = Schedule.recurs(5) && Schedule.exponential(1.second, 0.2)

  override def soldTickets(): ZIO[Database, Nothing, Int] = ZIO.accessM[Database](_.get.count())

  private def canBeReserved(newTicket: GameTicket): ZSTM[Database, String, Unit] = {
    val ticketExists: GameTicket => Boolean = gt => gt.seat.seat   == newTicket.seat.seat &&
                                                    gt.seat.sector == newTicket.seat.sector &&
                                                    gt.seat.row    == newTicket.seat.row &&
                                                    gt.game        == newTicket.game

    val outsideCapacity = newTicket.seat.row > noRows || newTicket.seat.seat > rowSize

    for {
     alreadySold <- Database.select(ticketExists)
      _          <- STM.fail(newTicket + " Incorrect seat or row!").when(outsideCapacity)
      _          <- STM.fail(newTicket + " Seat sold!").when(alreadySold.nonEmpty)
    } yield ()
  }

  //todo: remove not used
  private def printTryForTicket(tickets: Seq[GameTicket]) =
    for {
      l <- ZSTM.foreach(tickets)(t=>Database.exists(t))
      _ <- STM.succeed(tickets.foreach(t=>println(s"DeskId: ${t.deskId}, sold seat: $t"))).when(!l.exists(_.equals(false)))
    } yield ()

  private def reserveSeats(tickets: Seq[GameTicket]): ZSTM[Database, String, List[GameTicket]] = {
    for {
      _            <- ZSTM.foreach(tickets)(canBeReserved)
      _            <- ZSTM.foreach(tickets)(t=>Database.upsert(t))
    } yield tickets.toList
  }

  //todo: remove sector
  override def reserveSeats(deskId: Int, noOfSeats: Int, sectorName: String, game: String): ZIO[TicketsEnv, String, List[GameTicket]] =
    ZSTM.atomically(
      for {
        firstFree <- almostOKSeatSelector(game, noOfSeats)
        booked    <- {
                      val listOfSeats = (firstFree.seat.seat until firstFree.seat.seat + noOfSeats).map(nextSeat =>
                                firstFree.copy(seat = firstFree.seat.copy(seat = nextSeat), deskId = deskId))
                       reserveSeats(listOfSeats)
                     }
      } yield booked)
    .tapError(e=>ZIO.succeed(println("Error: " + e)))
    .retry(reserveSeatsRetryPolicy)


  override def reserveSeats(seats: Seq[Seat], game: String, supporter: Supporter): ZIO[TicketsEnv, String, List[GameTicket]] = {
    val tickets = seats.map{ seat => GameTicket(game, seat, supporter) }
    ZSTM.atomically(reserveSeats(tickets))
  }

  private def almostOKSeatSelector(game: String, noOfSeats: Int): ZSTM[Database, String, GameTicket] = {
    val sectors   = 'A' to 'U'
    def swapIfZero(v: Int) = if(v == 0) 1 else v
    def supporter = random.alphanumeric.take(10).mkString
    def row       = { val ch = random.nextInt(noRows);            swapIfZero(ch) }
    def seat      = { val ch = random.nextInt(rowSize-noOfSeats); swapIfZero(ch) }

    def generateTicket: ZSTM[Database, String, GameTicket] =
      STM.succeed(GameTicket(game,
                             Seat(Sector(sectors(Random.nextInt(21)).toString), row, seat),
                             Supporter(random.nextInt(100), supporter)))
    for {
      _            <- STM.fail("Incorrect capacity").when(rowSize - noOfSeats < 0)
      randomTicket <- generateTicket
      _            <- canBeReserved(randomTicket)
    } yield randomTicket
  }
}
