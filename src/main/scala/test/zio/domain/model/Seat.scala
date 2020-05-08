package test.zio.domain.model

import test.zio.domain.Database.Row

case class Seat(sector: Sector, row: Int, seat: Int) extends Row
