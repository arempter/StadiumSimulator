package test.zio.infrastructure

import test.zio.domain.Database
import test.zio.domain.model.{GameTicket, Sector}
import zio.{UIO, ZIO}
import zio.stm.{STM, TSet}

case class InMemoryDatabase(db: TSet[GameTicket]) extends Database.Service {

  // could accept sortOrder
  override def select(cond: GameTicket => Boolean): STM[Nothing, List[GameTicket]] =
    db.toList
      .map(l => l.filter(cond))

  override def exists(ticket: GameTicket): STM[Nothing, Boolean] = db.contains(ticket)

  override def insert(ticket: GameTicket): STM[Nothing, Unit] = ticket match {
    case row: GameTicket => db.put(row)
    case _               => STM.unit
  }

  override def count(): UIO[Int] = db.size.commit

  override def countByDesk(id: Int): UIO[Int] = {
    val byDesk: GameTicket => Boolean = gt => gt.deskId == id
    for {
      s <- select(byDesk).commit
      r <- UIO.succeed(s.count(_.deskId == id))
    } yield r
  }

  override def countBySector(): UIO[Map[Sector, Int]] =
    for {
      d <- select(_ => true).commit
      g <- ZIO.succeed(d.groupBy(_.seat.sector).view.mapValues(_.size).toMap)
    } yield g

}
