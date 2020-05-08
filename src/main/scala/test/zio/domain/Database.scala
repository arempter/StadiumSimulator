package test.zio.domain

import test.zio.domain.model.Seat
import test.zio.infrastructure.InMemoryDatabase
import zio.stm.{STM, TSet, ZSTM}
import zio.{Has, UIO, ZIO, ZLayer}

object Database {
  type Database = Has[Service]

  trait Row
  case class DBResult[A](record: A)

  trait Service {
    def select(cond: Seat => Boolean): STM[Nothing, List[DBResult[Seat]]]
    def exists(cond: Seat): STM[Nothing, Boolean]
    def upsert(row: Row): STM[Nothing, Unit]
    def count(): UIO[Int]
  }

  def select(cond: Seat => Boolean): ZSTM[Database, Nothing, List[DBResult[Seat]]] = ZSTM.accessM(_.get.select(cond))
  def exists(cond: Seat): ZSTM[Database, Nothing, Boolean] = ZSTM.accessM(_.get.exists(cond))
  def upsert(row: Row): ZSTM[Database, Nothing, Unit] = ZSTM.accessM(_.get.upsert(row))
  def allEntries(): ZIO[Database, Nothing, Int] = ZIO.accessM(_.get.count())

  val live = ZLayer.fromService[TSet[Seat], Service](db => InMemoryDatabase(db))

}
