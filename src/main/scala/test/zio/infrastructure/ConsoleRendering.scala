package test.zio.infrastructure

import org.fusesource.jansi.{Ansi, AnsiConsole}
import zio.ZIO

object ConsoleRendering {

  val noOfSeats    = 25
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
    val s = (1 to noOfSeats - 1).map(s=> if (s <10) s"0$s│" else s"$s│" ).mkString
    for (i <- 1 to noOfRows) {
      b.cursor(y0+i, x0+1).bgDefault().a(s"Row:$i")
      b.cursor(y0+i, x0+1+legendLength).bgGreen().a(s.concat(noOfSeats.toString)).reset().restoreCursorPosition()
    }
  }

  def markSold(b: Ansi, sold: List[Int], x0: Int, y0: Int) = {
    val soldSeatsPos = sold.map(p => (p*3)+x0+legendLength-2)
    for (c <- soldSeatsPos)
      b1.cursor(y0+1, c).bgRed().a("XX").reset().restoreCursorPosition()
  }

  // seat size + delimiters
  val boxWith = 25*2+24+2
  val b0 = Ansi.ansi().saveCursorPosition().eraseScreen()
  val b1 = drawSector(b0, x0, y0, boxWith+legendLength, 12)

//  drawHeader(b1, "A", x0, y0)
//  drawSeats(b1, noOfSeats, x0+legendLength, y0, 1)
//  drawSeats(b1, noOfSeats, x0+legendLength, y0+1, 2)
//  drawRows(b1, x0, y0, 10)
//  markSold(b1, List(10,11,12,13,21,25), x0, y0)
//  markSold(b1, List(8,20,15), x0, y0+3)
//  AnsiConsole.out.println(b1)


  def showSector(sector: String, sold: List[Int]): ZIO[Any, Throwable, Unit] =
    for {
      _ <- ZIO.effect(drawHeader(b1, sector, x0, y0))
      _ <- ZIO.effect(drawRows(b1, x0, y0, 10))
      _ <- ZIO.effect(markSold(b1, sold, x0, y0))
      _ <- ZIO.effect(AnsiConsole.out.println(b1))
    } yield ()

}
