package test.zio.domain

import test.zio.domain.model.{GameTicket, Sector}
import test.zio.infrastructure.InMemoryDatabase
import zio.stm.{STM, TSet, ZSTM}
import zio.{Has, UIO, ZIO, ZLayer}

object Database {
  type Database = Has[Service]

  trait Service {
    def select(cond: GameTicket => Boolean): STM[Nothing, List[GameTicket]]
    def exists(cond: GameTicket): STM[Nothing, Boolean]
    def upsert(ticket: GameTicket): STM[Nothing, Unit]
    def count(): UIO[Int]
    def countByDesk(id: Int): UIO[Int]
    def countBySector(): UIO[Map[Sector, Int]]
  }

  def select(cond: GameTicket => Boolean): ZSTM[Database, Nothing, List[GameTicket]] = ZSTM.accessM(_.get.select(cond))
  def exists(cond: GameTicket): ZSTM[Database, Nothing, Boolean] = ZSTM.accessM(_.get.exists(cond))
  def upsert(ticket: GameTicket): ZSTM[Database, Nothing, Unit] = ZSTM.accessM(_.get.upsert(ticket))
  def count(): ZIO[Database, Nothing, Int] = ZIO.accessM(_.get.count())
  def countByDesk(id: Int): ZIO[Database, Nothing, Int] = ZIO.accessM(_.get.countByDesk(id))
  def countBySector(): ZIO[Database, Nothing, Map[Sector, Int]] = ZIO.accessM(_.get.countBySector())

  val live = ZLayer.fromService[TSet[GameTicket], Service](db => InMemoryDatabase(db))

}
