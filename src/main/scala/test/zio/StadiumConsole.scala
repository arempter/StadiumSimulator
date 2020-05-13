package test.zio
import test.zio.infrastructure.ConsoleRendering.showSector
import zio.console.Console
import zio._

object StadiumConsole extends zio.App {

  //_    <- console.putStrLn("\u001b[H\u001b[2J")

  val getInputFromUser: RIO[Console, String] = for {
    _    <- console.putStrLn("Menu")
    _    <- console.putStrLn("S to select sector...")
    _    <- console.putStrLn("B to buy tickets")
    _    <- console.putStrLn("Q to exit")
    input <- zio.console.getStrLn
  } yield input

  def parseUserInput(i: String) =  i match {
    case "s" =>
      for {
        _    <- console.putStrLn("Select sector...")
        s    <- zio.console.getStrLn
        _    <- showSector(s, Nil)
      } yield ()
    case "b" =>
      for {
        _   <- console.putStrLn("Buying...")
        s   <- zio.console.getStrLn
      } yield ()
    case "q" => ZIO.fail("closing console...")
    case _   => getInputFromUser
  }


  val schedule = Schedule.doWhile[String](s=> !equals("Q"))
  val menuProgram =
    (for {
      i <- getInputFromUser
      _ <- parseUserInput(i)
    } yield()).forever

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    menuProgram.foldM(
      e => console.putStrLn("Exit: " + e) *> IO.succeed(0),
      _ => IO.succeed(0)
    ).provideLayer(console.Console.live)
}
