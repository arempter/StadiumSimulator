package test.zio

import test.zio.application.StadiumSimulator.ticketDeskSimulatorProgram
import test.zio.domain.model.GameTicket
import test.zio.domain.{Database, Tickets}
import zio._
import zio.stm.TSet

object RunStadiumSimulator extends zio.App {

  val db         = TSet.empty[GameTicket]
  val dbLayer    = db.commit.toLayer >>> Database.live
  val programEnv = dbLayer ++ Tickets.live ++ console.Console.live ++ clock.Clock.live

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    ticketDeskSimulatorProgram.foldM(e => console.putStrLn("Failed: " + e) *> IO.succeed(0),
                                     _ => IO.succeed(0)
    ).provideLayer(programEnv)
}
