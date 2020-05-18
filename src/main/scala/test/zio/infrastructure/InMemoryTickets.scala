package test.zio.infrastructure

import test.zio.domain.Database.Database
import test.zio.domain.Tickets.Tickets
import test.zio.domain.model.{GameTicket, Seat, Sector, Supporter}
import test.zio.domain.{Database, Tickets}
import zio.clock.Clock
import zio.{Schedule, ZIO}
import zio.stm.{STM, ZSTM}
import zio.duration._

import scala.util.Random

case class InMemoryTickets() extends Tickets.Service {

  private val rowSize                 = 25
  private val noRows                  = 10
  private val random                  = Random
  private val reserveSeatsRetryPolicy = Schedule.recurs(5) && Schedule.exponential(1.second, 0.2)

  override def soldTickets(): ZIO[Database, Nothing, Int] = ZIO.accessM[Database](_.get.count())

  private def canBeReserved(isSold: Boolean, newTicket: GameTicket, existingTickets: List[GameTicket]): ZSTM[Any, String, Unit] = {
    val outsideCapacity = newTicket.seat.row > noRows || newTicket.seat.seat > rowSize
    if(isSold)
      STM.fail("Already sold!")
    else if (outsideCapacity)
      STM.fail("Incorrect seat or row!")
    else if (existingTickets.nonEmpty)
      STM.fail("Seat already sold!")
    else STM.succeed()
  }

  private def reserveSeat(ticket: GameTicket): ZSTM[Database, String, GameTicket] = {
    //todo: deduplicate
    val ticketSold: GameTicket => Boolean = gt =>
                                            gt.seat.seat   == ticket.seat.seat &&
                                            gt.seat.sector == ticket.seat.sector &&
                                            gt.seat.row    == ticket.seat.row &&
                                            gt.game        == ticket.game

    for {
      isSold   <- Database.exists(ticket)
      seatSold <- Database.select(ticketSold)
      _        <- STM.succeed(println(s"DeskId: ${ticket.deskId}, trying to reserve seat: $ticket")) *> canBeReserved(isSold, ticket, seatSold)
      _        <- Database.upsert(ticket)
    } yield ticket
  }
  override def reserveSeats(deskId: Int, noOfSeats: Int, sectorName: String, game: String): ZIO[Database with Clock, String, List[GameTicket]] =
    ZSTM.atomically(
      for {
        firstFree <- almostOKSeatSelector(game, noOfSeats, sectorName)
        booked    <- ZSTM.foreach(firstFree.seat.seat until firstFree.seat.seat + noOfSeats) { nextSeat =>
                                    reserveSeat(firstFree.copy(seat = firstFree.seat.copy(seat = nextSeat), deskId = deskId))
        }
      } yield booked)
      .retry(reserveSeatsRetryPolicy)

  override def reserveSeats(seats: Seq[Seat], game: String, supporter: Supporter): ZIO[Database, String, List[GameTicket]] =
    ZSTM.atomically(
      ZSTM.foreach(seats) { seat => reserveSeat(GameTicket(game, seat, supporter)) })

  private def almostOKSeatSelector(game: String, noOfSeats: Int, sectorName: String): ZSTM[Database, String, GameTicket] = {
    val sectors   = 'A' to 'U'
    def supporter = random.alphanumeric.take(10).mkString
    def row       = { val ch = random.nextInt(noRows); if (ch == 0) 1 else ch }
    def seat      = { val ch = random.nextInt(rowSize-noOfSeats); if (ch == 0) 1 else ch }

    def generateTicket: ZSTM[Database, String, GameTicket] =
      STM.succeed(GameTicket(game,
                             Seat(Sector(sectors(Random.nextInt(21)).toString), row, seat),
                             Supporter(random.nextInt(100), supporter)))
    for {
      randomTicket <- generateTicket
      isSold       <- Database.exists(randomTicket)
      seatSold     <- Database.select(gt =>
                                      gt.seat.seat   == randomTicket.seat.seat &&
                                      gt.seat.sector == randomTicket.seat.sector &&
                                      gt.seat.row    == randomTicket.seat.row &&
                                      gt.game        == randomTicket.game)
      _            <- canBeReserved(isSold, randomTicket, seatSold)
    } yield randomTicket
  }
}
