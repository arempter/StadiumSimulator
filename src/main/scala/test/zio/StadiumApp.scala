package test.zio

import test.zio.domain.SectorsRepository.SectorRepository
import test.zio.application.StadiumSimulator.{soldSummary, ticketService}
import test.zio.domain.{Database, SectorsRepository, Tickets}
import test.zio.domain.Tickets.TicketService
import test.zio.domain.model.GameTicket
import zio._
import zio.stm.TSet

import scala.util.Random

object StadiumApp extends zio.App {

  val db         = TSet.empty[GameTicket]
  val dbLayer    = db.commit.toLayer >>> Database.live
  val programEnv = dbLayer ++ Tickets.live ++ SectorsRepository.live ++ console.Console.live ++ clock.Clock.live
  val random     = Random

  val program: ZIO[SectorRepository with TicketService, String, Unit] =
    (ticketService(1, random.nextInt(10), random.nextInt(6)) &>
      ticketService(2, random.nextInt(10), random.nextInt(6)) &>
      ticketService(3, random.nextInt(10), random.nextInt(6)) &>
      ticketService(4, random.nextInt(10), random.nextInt(6))
      ) *> soldSummary

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    program.foldM(e => console.putStrLn("Failed: " + e) *> IO.succeed(0),
                  _ => IO.succeed(0)
    ).provideLayer(programEnv)
}
