package test.zio.infrastructure

import test.zio.domain.Database.Database
import test.zio.domain.model.{GameTicket, Seat, Sector, Supporter}
import test.zio.domain.{Database, SectorsRepository}
import zio.ZIO
import zio.clock.Clock
import zio.stm.{STM, ZSTM}

import scala.util.Random

case class InMemorySectorsRepository() extends SectorsRepository.Service {

  // todo: capacity per Sector && some validation
  val rowSize = 25
  val noRows = 25
  val NO_FREE_SEATS = "No free seats available in this Sector"

  private val random = Random

  private def reserveSeat(ticket: GameTicket): ZSTM[Database, String, GameTicket] = {
    val outsideCapacity = ticket.seat.row > noRows || ticket.seat.seat > rowSize

    for {
      isSold <- Database.exists(ticket)
      _      <- STM.succeed(println(s"Reserving seat $ticket"))
      _      <- Database.upsert(ticket).when(!isSold && !outsideCapacity)
    } yield ticket
  }

  override def reserveSeats(noOfSeats: Int, sectorName: String, game: String): ZIO[Database with Clock, String, List[GameTicket]] =
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
    STM.succeed(GameTicket(s"game${random.nextInt(3)}",
      Seat(Sector(sectors(random.nextInt(21)).toString), random.nextInt(noRows), random.nextInt(rowSize)),
      Supporter(random.alphanumeric.take(10).mkString, supporter)))
  }

}
