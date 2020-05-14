package test.zio

import test.zio.application.StadiumSimulator.{soldSummary, ticketsOffice}
import test.zio.domain.Tickets.Tickets
import test.zio.domain.model.GameTicket
import test.zio.domain.{Database, Tickets}
import zio._
import zio.clock.Clock
import zio.stm.TSet
import zio.duration._

import scala.util.Random

object RunStadiumSimulator extends zio.App {

  val db         = TSet.empty[GameTicket]
  val dbLayer    = db.commit.toLayer >>> Database.live
  val programEnv = dbLayer ++ Tickets.live ++ console.Console.live ++ clock.Clock.live
  val random     = Random
  val game       = s"game${random.nextInt(3)}"

  val schedule: Schedule[Clock, Any, (Int, Duration)] = Schedule.recurs(5) && Schedule.exponential(100.milliseconds, 0.2)
  val handleFailure: Int => ZIO[Any, Nothing, Unit] = id => IO.succeed(println(s"Id: $id Failed to book")) *> IO.succeed()

  val program: ZIO[Tickets, String, Unit] =
    (ticketsOffice(1, random.nextInt(3), random.nextInt(4), game).retry(schedule).orElse(handleFailure(1)) &>
      ticketsOffice(2, random.nextInt(3), random.nextInt(4), game).retry(schedule).orElse(handleFailure(2)) &>
      ticketsOffice(3, random.nextInt(3), random.nextInt(4), game).retry(schedule).orElse(handleFailure(3)) &>
      ticketsOffice(4, random.nextInt(3), random.nextInt(4), game).retry(schedule).orElse(handleFailure(4))
      ) *> soldSummary

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    program.foldM(e => console.putStrLn("Failed: " + e) *> IO.succeed(0),
                  _ => IO.succeed(0)
    ).provideLayer(programEnv)
}
