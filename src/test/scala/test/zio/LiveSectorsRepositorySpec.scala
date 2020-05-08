package test.zio

import _root_.test.zio.domain.model.{Seat, Sector}
import _root_.test.zio.domain.{Database, SectorsRepository}
import zio.stm.{STM, TSet}
import zio.test.Assertion._
import zio.test._


object LiveSectorsRepositorySpec extends DefaultRunnableSpec {
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("LiveSectorsRepositorySpec")(testReserveSeats, testReserveSeatsWithEmptyDB)

  val sectorA = Sector("A")
  val sectorB = Sector("B")

  val db = TSet.make[Seat](
    Seat(sectorB, 1, 1), Seat(sectorB, 1, 4), Seat(sectorB, 1, 12), Seat(sectorB, 1, 16), Seat(sectorB, 1, 20),
    Seat(sectorB, 2, 1), Seat(sectorB, 2, 4), Seat(sectorB, 2, 12),
    Seat(sectorB, 3, 1), Seat(sectorB, 3, 4), Seat(sectorB, 3, 8),
    Seat(sectorA, 3, 1), Seat(sectorA, 3, 4), Seat(sectorA, 3, 8),
    Seat(sectorA, 1, 1), Seat(sectorA, 1, 4), Seat(sectorA, 1, 8)
  )
  val emptyDb = TSet.empty[Seat]

  private def testEnv(db: STM[Nothing,TSet[Seat]]) = {
    val dbLayer = db.commit.toLayer >>> Database.live
    dbLayer ++ SectorsRepository.live
  }

  val testReserveSeats = suite("Ticket reservation in non empty db")(
    testM("reserveSeats should return seats seq that can be purchased") {
      val res = SectorsRepository.reserveSeats(2, sectorA.name).provideLayer(testEnv(db))
      assertM(res)(equalTo(List(Seat(sectorA, 1, 2), Seat(sectorA, 1, 3))))
    },
    testM("parallel reserveSeats should return seats seq that can be purchased") {
      val res = for {
        f1 <- SectorsRepository.reserveSeats(2, "A").fork
        f2 <- SectorsRepository.reserveSeats(2, "A").fork
        f3 <- SectorsRepository.reserveSeats(2, "A").fork
        r1 <- f1.join
        r2 <- f2.join
        r3 <- f3.join
      } yield {
          assert(r1)(isNonEmpty) &&
          assert(r2)(isNonEmpty) &&
          assert(r3)(isNonEmpty)
      }
      res.provideLayer(testEnv(db))
    },
    testM("reserveSeats to many seats should fail"){
      for {
        res <- SectorsRepository.reserveSeats(26, sectorA.name).provideLayer(testEnv(db)).flip
      } yield assert(res)(equalTo("No free seats available in this Sector"))
    },
    testM("findFreeSeats should retrun first seats in each row") {
      val res = SectorsRepository.findFreeSeats(3, sectorB.name).commit.provideLayer(testEnv(db))
      assertM(res)(equalTo(List(Seat(sectorB, 1, 5), Seat(sectorB, 2, 5), Seat(sectorB, 3, 5))))
    }
  )
  val testReserveSeatsWithEmptyDB = suite("Ticket reservation in empty db")(
    testM("reserve seats should retrun seat seq on random start") {
      val res = SectorsRepository.reserveSeats(5, sectorA.name).provideLayer(testEnv(emptyDb))
      assertM(res.map(_.size))(equalTo(5))
    })

}

