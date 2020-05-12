package test.zio.infrastructure

import test.zio.domain.Database.Database
import test.zio.domain.Tickets
import test.zio.domain.model.GameTicket
import zio.{UIO, ZIO}

case class InMemoryTicketService() extends Tickets.Service {
  override def sellTickets(matchTickets: Seq[GameTicket]): UIO[Seq[GameTicket]] = ???

  override def soldTickets(): ZIO[Database, Nothing, Int] = ZIO.accessM[Database](_.get.count())

}
