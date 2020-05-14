package test.zio

import test.zio.application.StadiumSimulator.{soldSummary, ticketsOffice}
import test.zio.domain.Tickets.Tickets
import test.zio.domain.model.GameTicket
import test.zio.domain.{Database, Tickets}
import zio._
import zio.stm.TSet

import scala.util.Random

object RunStadiumSimulator extends zio.App {

  val db         = TSet.empty[GameTicket]
  val dbLayer    = db.commit.toLayer >>> Database.live
  val programEnv = dbLayer ++ Tickets.live ++ console.Console.live ++ clock.Clock.live
  val random     = Random
  val game       = s"game${random.nextInt(3)}"

  //todo: will not fully work now
  val program: ZIO[Tickets, String, Unit] =
    (ticketsOffice(1, random.nextInt(10), random.nextInt(6), game) &>
      ticketsOffice(2, random.nextInt(10), random.nextInt(6), game) &>
      ticketsOffice(3, random.nextInt(10), random.nextInt(6), game) &>
      ticketsOffice(4, random.nextInt(10), random.nextInt(6), game)
      ) *> soldSummary

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    program.foldM(e => console.putStrLn("Failed: " + e) *> IO.succeed(0),
                  _ => IO.succeed(0)
    ).provideLayer(programEnv)
}
