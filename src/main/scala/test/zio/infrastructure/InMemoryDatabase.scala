package test.zio.infrastructure

import test.zio.domain.Database
import test.zio.domain.model.{GameTicket, Seat}
import zio.UIO
import zio.stm.{STM, TSet}

case class InMemoryDatabase(db: TSet[GameTicket]) extends Database.Service {

  // could accept sortOrder
  override def select(cond: GameTicket => Boolean): STM[Nothing, List[GameTicket]] =
    db.toList
      .map(l=> l.filter(cond))

  override def exists(cond: GameTicket): STM[Nothing, Boolean] = db.contains(cond)

  override def upsert(ticket: GameTicket): STM[Nothing, Unit] = ticket match {
    case row: GameTicket => db.put(row)
    case         _ => STM.unit
  }

  override def count(): UIO[Int] = db.size.commit
}
