package test.zio.domain

import test.zio.domain.Database.Database
import test.zio.domain.model.MatchTicket
import test.zio.infrastructure.InMemoryTicketService
import zio.{Has, UIO, ZIO, ZLayer}

object Tickets {
  type TicketService = Has[Service] with Database

  // capacity - number of ticket desks

  trait Service {
    def sellTickets(matchTickets: Seq[MatchTicket]): UIO[Seq[MatchTicket]]
    def soldTickets(): ZIO[Database, Nothing, Int]
  }

  def soldTickets(): ZIO[TicketService, Nothing, Int] = ZIO.accessM(_.get.soldTickets())

  val live = ZLayer.succeed[Service](InMemoryTicketService())

}
