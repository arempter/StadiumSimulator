package test.zio.infrastructure

import test.zio.domain.Database
import test.zio.domain.Database.{DBResult, Row}
import test.zio.domain.model.Seat
import zio.UIO
import zio.stm.{STM, TSet}

case class InMemoryDatabase(db: TSet[Seat]) extends Database.Service {

  // could accept sortOrder
  override def select(cond: Seat => Boolean): STM[Nothing, List[DBResult[Seat]]] =
    db.toList
      .map(l=> l.filter(cond).map(r=>DBResult(r)))

  override def exists(cond: Seat): STM[Nothing, Boolean] = db.contains(cond)

  override def upsert(row: Row): STM[Nothing, Unit] = row match {
    case row: Seat => db.put(row)
    case         _ => STM.unit
  }

  override def count(): UIO[Int] = db.size.commit
}
