package test.zio
import test.zio.domain.Database.Database
import test.zio.domain.Rendering.Rendering
import test.zio.domain.SectorsRepository.SectorRepository
import test.zio.domain.model.GameTicket
import test.zio.domain.{Database, Rendering, SectorsRepository}
import zio._
import zio.console.Console
import zio.stm.TSet

object StadiumConsole extends zio.App {

  val db         = TSet.empty[GameTicket]
  val dbLayer    = db.commit.toLayer >>> Database.live
  val programEnv = dbLayer ++ SectorsRepository.live ++ console.Console.live ++ clock.Clock.live ++ Rendering.live

  val displayMainMenu: RIO[Console, String] = for {
    _    <- console.putStrLn("Menu")
    _    <- console.putStrLn("S to select sector...")
    _    <- console.putStrLn("B to buy tickets")
    _    <- console.putStrLn("Q to exit")
    input <- zio.console.getStrLn
  } yield input

  //todo: Add some validation to input
  def menuOptions(i: String): ZIO[Console with SectorRepository with Rendering, Serializable, Any] =  i match {
    case "s" => sectorsMenu
    case "b" => ticketMenu
    case "q" => ZIO.fail("closing console...")
    case _   => displayMainMenu
  }

  val sectorsMenu: ZIO[Database with Rendering with Console, Throwable, Unit] =
    for {
      _    <- console.putStrLn("Select sector...")
      s    <- zio.console.getStrLn
      sold <- {
        val selectInSector: GameTicket => Boolean = gt => gt.seat.sector.name == s
        Database.select(selectInSector).commit
          .map(_.groupBy(_.seat.row)).map(_.view.mapValues(_.map(_.seat.seat)))
      }
      _    <- Rendering.showSector(s, sold)
    } yield ()

  val ticketMenu =
    for {
      _   <- console.putStrLn("Buying tickets...")
      _   <- console.putStrLn("What game?")
      g   <- zio.console.getStrLn
      _   <- console.putStrLn("How many tickets?")
      t   <- zio.console.getStrLn
      _   <- console.putStrLn("Preferred sector?")
      s   <- zio.console.getStrLn
      _   <- SectorsRepository.reserveSeats(t.toInt, s, g)
    } yield ()

  val menuProgram =
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
