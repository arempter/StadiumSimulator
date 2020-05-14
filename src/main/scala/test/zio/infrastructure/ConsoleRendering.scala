package test.zio.infrastructure

import org.fusesource.jansi.{Ansi, AnsiConsole}
import zio.ZIO

object ConsoleRendering {

  val noOfSeats    = 25
  val noOfRows     = 10
  val (x0, y0)     = (5, 10)
  val legendLength = 7

  def drawSector(b: Ansi, x0: Int, y0: Int, w: Int, h: Int) = {
    val topStr    = "┌".concat("─" * (w - 2)).concat("┐")
    val wallStr   = "│".concat(" " * (w - 2)).concat("│")
    val bottomStr = "└".concat("─" * (w - 2)).concat("┘")

    val top = b.cursor(y0, x0).a(topStr)
    val walls = (0 to h - 2).toList.foldLeft(top) { (b: Ansi, i: Int) =>
      b.cursor(y0 + i + 1, x0).a(wallStr)
    }
    walls.cursor(y0 + h - 1, x0).a(bottomStr)
  }

  def drawHeader(b: Ansi, sector: String, x0: Int, y0: Int) = b.cursor(y0 - 1, x0 + 1).a(s"Sector $sector")

  def drawRows(b: Ansi, x0: Int, y0: Int, noOfRows: Int) = {
    val s = (1 to noOfSeats - 1).map(s=> if (s < 10 ) s"0$s│" else s"$s│" ).mkString
    for (i <- 1 to noOfRows) {
      b.cursor(y0 + i, x0 + 1).bgDefault().a(s"Row:$i")
      b.cursor(y0 + i, x0 + 1 + legendLength).bgGreen().a(s.concat(noOfSeats.toString)).reset().restoreCursorPosition()
    }
  }

  def markSold(b: Ansi, sold: List[Int], x0: Int, y0: Int) = {
    val soldSeatsPos = sold.map(p => ( p * 3 ) +x0 + legendLength - 2)
    for (c <- soldSeatsPos)
      b.cursor(y0 + 1, c).bgRed().a("XX").reset().restoreCursorPosition()
  }

  // seat size + delimiters
  val boxWith   = noOfSeats * 2 + noOfSeats + 2
  val boxHeight = noOfRows + 2
  val b0        = Ansi.ansi().saveCursorPosition().eraseScreen()
  val b1        = drawSector(b0, x0, y0, boxWith+legendLength, boxHeight)

  def showSector(sector: String, sold: List[Int]): ZIO[Any, Throwable, Unit] =
    for {
      _ <- ZIO.effect(drawHeader(b1, sector, x0, y0))
      _ <- ZIO.effect(drawRows(b1, x0, y0, noOfRows))
      _ <- ZIO.effect(markSold(b1, sold, x0, y0))
      _ <- ZIO.effect(AnsiConsole.out.println(b1))
    } yield ()

}
