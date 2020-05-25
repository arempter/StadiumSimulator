package test.zio
import java.io.IOException

import test.zio.application.StadiumSimulator.ticketDeskSimulatorProgram
import test.zio.domain.Database.Database
import test.zio.domain.Rendering.Rendering
import test.zio.domain.Tickets.{Tickets, TicketsEnv}
import test.zio.domain.model.{GameTicket, Seat, Sector, Supporter}
import test.zio.domain.{Database, Rendering, Tickets}
import zio._
import zio.console.Console
import zio.stm.TSet

object StadiumConsole extends zio.App {

  val db         = TSet.empty[GameTicket]
  val dbLayer    = db.commit.toLayer >>> Database.live
  val programEnv = dbLayer ++ Tickets.live ++ console.Console.live ++ clock.Clock.live ++ Rendering.live

  val displayMainMenu: RIO[Console, String] = for {
    _    <- console.putStrLn("\nMenu")
    _    <- console.putStrLn("s - to select sector...")
    _    <- console.putStrLn("b - to buy tickets")
    _    <- console.putStrLn("r - to run simulation")
    _    <- console.putStrLn("q - to exit")
    input <- zio.console.getStrLn
  } yield input

  def menuOptions(i: String): ZIO[Tickets with TicketsEnv with Rendering with Console, Serializable, Any] =  i match {
    case "s" => sectorsMenu
    case "b" => ticketMenu
    case "r" => simulationMenu
    case "q" => ZIO.fail("Closing console...")
    case _   => displayMainMenu
  }

  def simulationMenu: ZIO[Tickets with TicketsEnv, String, Unit] = ticketDeskSimulatorProgram

  val sectorsMenu: ZIO[Database with Rendering with Console, Throwable, Unit] =
    for {
      _    <- console.putStrLn("Select sector...")
      s    <- zio.console.getStrLn
      sold <- {
               val inSector: GameTicket => Boolean = gt => gt.seat.sector.name == s.toUpperCase
               Database.select(inSector).commit
                .map(_.groupBy(_.seat.row)).map(_.view.mapValues(_.map(_.seat.seat)))
      }
      _    <- zio.console.putStrLn(sold.keys.toString() + sold.values.toString())
      _    <- Rendering.showSector(s.toUpperCase, sold)
    } yield ()

  //todo: Add some validation to input
  def parseTicketInput(seats: String, sector: String, row: Int): List[Seat] = {
    val seatsP = seats.split(",").toList
    seatsP.map(s=>Seat(Sector(sector), row, s.toInt))
  }

  val ticketMenu: ZIO[Console with Tickets with TicketsEnv, IOException, Unit] =
    for {
      _         <- console.putStrLn("Buying tickets...")
      _         <- console.putStrLn("What game?")
      game      <- zio.console.getStrLn
      _         <- console.putStrLn("Supporter Id")
      id        <- zio.console.getStrLn
      _         <- console.putStrLn("Supporter Name")
      name      <- zio.console.getStrLn
      _         <- console.putStrLn("Sector?")
      sector    <- zio.console.getStrLn
      _         <- console.putStrLn("Which row?")
      row       <- zio.console.getStrLn
      _         <- console.putStrLn("Which seats?, comma separated")
      seats     <- zio.console.getStrLn
      _         <- Tickets.reserveSeats(parseTicketInput(seats, sector.toUpperCase, row.toInt), game, Supporter(id.toInt, name))
                  .catchAll(e=>console.putStrLn(e) *> IO.succeed())
    } yield ()

  val menuProgram: ZIO[Tickets with TicketsEnv with Rendering with Console, Serializable, Nothing] =
    (for {
      i <- displayMainMenu
      _ <- menuOptions(i)
    } yield()).forever

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    menuProgram.foldM(
      e => console.putStrLn("Exit: " + e) *> IO.succeed(0),
      _ => IO.succeed(0)
    ).provideLayer(programEnv)
}
