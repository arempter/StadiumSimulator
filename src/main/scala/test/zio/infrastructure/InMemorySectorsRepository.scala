package test.zio.infrastructure

import test.zio.domain.Database.Database
import test.zio.domain.model.{Seat, Sector}
import test.zio.domain.{Database, SectorsRepository}
import zio.ZIO
import zio.stm.{STM, ZSTM}

import scala.annotation.tailrec
import scala.util.Random

case class InMemorySectorsRepository() extends SectorsRepository.Service {

  // todo: capacity per Sector
  val rowSize = 25
  val noRows = 25

  private def reserveSeat(seat: Seat): ZSTM[Database, String, Seat] =
    for {
      isSold <- Database.exists(seat)
      _ <- if (isSold) STM.fail("Seat already sold") else STM.succeed(isSold)
      _ <- if (seat.row > noRows || seat.seat > rowSize) STM.fail("Invalid seat definition") else STM.succeed(isSold)
      _ <- Database.upsert(seat)
    } yield seat

  override def reserveSeats(noOfSeats: Int, sectorName: String): ZIO[Database, String, List[Seat]] =
    ZSTM.atomically(
      for {
        freeSeats <- findFreeSeats(noOfSeats, sectorName)
        _         <- if(freeSeats.isEmpty) STM.fail("No free seats available in this Sector") else STM.succeed(freeSeats)
        firstFree = freeSeats.headOption.map(_.seat).orElse(Some(0)).get
        booked        <-  ZSTM.foreach(firstFree until firstFree + noOfSeats) { nextSeat =>
                            reserveSeat(freeSeats.head.copy(seat = nextSeat))
                          }

    } yield booked)

  @tailrec
  private def findInRow(noOfSeats: Int, db: List[Seat], last: Seat, acc: List[Seat] = Nil): List[Seat] =
    db match {
    case f :: n :: _ if (n.seat - (f.seat + 1)) >= noOfSeats => n.copy(seat = f.seat + 1) :: acc
    case _ :: rest                                           => findInRow(noOfSeats, rest, last, acc)
    case Nil if (last.seat + noOfSeats) <= rowSize           => last.copy(seat = last.seat + 1) :: acc
    case Nil                                                 => acc
  }

  override def findFreeSeats(noOfSeats: Int, sectorName: String): ZSTM[Database, String, List[Seat]] = {
    val allFreeInSector: Seat => Boolean = s => s.sector.name == sectorName
    val random = Random

    for {
      seats <- Database.select(allFreeInSector)
      r     <-
              if(seats.isEmpty){
                STM.succeed(List(Seat(Sector(sectorName),random.nextInt(noRows),random.nextInt(rowSize))))
              } else {
                STM.foreach(seats
                  .map(_.record)
                  .groupBy(_.row)) { case (_, l) =>
                  val sortedSeats = l.sortBy(_.seat)
                  STM.succeed(findInRow(noOfSeats, sortedSeats, sortedSeats.last))
                }.map(_.flatten)
              }
    } yield r
  }

}
