package test.zio.infrastructure

import test.zio.domain.Database.Database
import test.zio.domain.{Database, Tickets}
import test.zio.domain.Tickets.TicketService
import test.zio.domain.model.{GameTicket, Seat, Sector, Supporter}
import zio.ZIO
import zio.stm.{STM, ZSTM}

import scala.util.Random

case class InMemoryTicketService() extends Tickets.Service {

  val rowSize = 25
  val noRows = 10
  val NO_FREE_SEATS = "No free seats available in this Sector"

  private val random = Random

  override def soldTickets(): ZIO[Database, Nothing, Int] = ZIO.accessM[Database](_.get.count())

  private def reserveSeat(ticket: GameTicket): ZSTM[Database, String, GameTicket] = {
    val outsideCapacity = ticket.seat.row > noRows || ticket.seat.seat > rowSize

    for {
      isSold <- Database.exists(ticket)
      _      <- STM.succeed(println(s"Reserving seat $ticket"))
      _      <- Database.upsert(ticket).when(!isSold && !outsideCapacity)
    } yield ticket
  }


  override def reserveSeats(noOfSeats: Int, sectorName: String, game: String): ZIO[TicketService, String, List[GameTicket]] =
    ZSTM.atomically(
      for {
        firstFree     <- findFreeSeat(noOfSeats, sectorName)
        booked        <- ZSTM.foreach(firstFree.seat.seat until firstFree.seat.seat + noOfSeats) { nextSeat =>
          reserveSeat(firstFree.copy(seat = firstFree.seat.copy(seat = nextSeat)))
        }
      } yield booked)

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
