package test.zio.infrastructure

import test.zio.domain.Database.Database
import test.zio.domain.Tickets.Tickets
import test.zio.domain.model.{GameTicket, Seat, Sector, Supporter}
import test.zio.domain.{Database, Tickets}
import zio.ZIO
import zio.stm.{STM, ZSTM}

import scala.util.Random

case class InMemoryTickets() extends Tickets.Service {

  val rowSize = 25
  val noRows = 10
  val NO_FREE_SEATS = "No free seats available in this Sector"

  private val random = Random

  override def soldTickets(): ZIO[Database, Nothing, Int] = ZIO.accessM[Database](_.get.count())

  private def canBeReserved(isSold: Boolean, newTicket: GameTicket, existingTickets: List[GameTicket]): ZSTM[Any, String, Unit] = {
    val outsideCapacity = newTicket.seat.row > noRows || newTicket.seat.seat > rowSize
    if(isSold) STM.fail("Already sold!")
    else if (outsideCapacity) STM.fail("Incorrect seat or row!")
    else if (existingTickets.nonEmpty) STM.fail("Seat already sold!") else STM.succeed()
  }

  private def reserveSeat(ticket: GameTicket, deskId: Int = 0): ZSTM[Database, String, GameTicket] = {
    val ticketSold: GameTicket => Boolean = gt => gt.seat.seat == ticket.seat.seat

    for {
      isSold   <- Database.exists(ticket)
      seatSold <- Database.select(ticketSold)
      _        <- STM.succeed(println(s"DeskId: $deskId, trying to reserve seat: $ticket")) *> canBeReserved(isSold, ticket, seatSold)
      _        <- Database.upsert(ticket)
    } yield ticket
  }

  override def reserveSeats(deskId: Int, noOfSeats: Int, sectorName: String, game: String): ZIO[Tickets, String, List[GameTicket]] =
    ZSTM.atomically(
      for {
        firstFree <- findFreeSeat(noOfSeats, sectorName)
        booked <- ZSTM.foreach(firstFree.seat.seat until firstFree.seat.seat + noOfSeats) { nextSeat =>
          reserveSeat(firstFree.copy(seat = firstFree.seat.copy(seat = nextSeat)), deskId)
        }
      } yield booked)

  override def reserveSeats(seats: Seq[Seat], game: String, supporter: Supporter): ZIO[Tickets, String, List[GameTicket]] =
    ZSTM.atomically(
      ZSTM.foreach(seats) { seat => reserveSeat(GameTicket(game, seat, supporter)) }
    )

  //todo: refactor
  private def findFreeSeat(noOfSeats: Int, sectorName: String): ZSTM[Database, String, GameTicket] = {
    val sectors = 'A' to 'U'

    def supporter = random.alphanumeric.take(10).mkString

    def row = {
      val ch = random.nextInt(noRows)
      if (ch == 0) 1 else ch
    }

    def seat = {
      val ch = random.nextInt(rowSize)
      if (ch == 0) 1 else ch
    }

    STM.succeed(GameTicket(s"game${random.nextInt(3)}",
      Seat(Sector(sectors(random.nextInt(21)).toString), row, seat),
      Supporter(random.alphanumeric.take(10).mkString, supporter)))
  }
}
