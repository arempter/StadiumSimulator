package test.zio

import _root_.test.zio.domain.model.{GameTicket, Seat, Sector, Supporter}
import _root_.test.zio.domain.{Database, SectorsRepository}
import zio.ZIO
import zio.stm.{STM, TSet}
import zio.test.Assertion._
import zio.test._


object LiveSectorsRepositorySpec extends DefaultRunnableSpec {
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("LiveSectorsRepositorySpec")(testReserveSeats, testReserveSeatsWithEmptyDB)

  val sectorA: Sector = Sector("A")
  val sectorB: Sector = Sector("B")

  val matchTicket: GameTicket = GameTicket("game1", Seat(sectorB, 1, 1), Supporter("123", "s1"))

  val db: STM[Nothing, TSet[GameTicket]] = TSet.make[GameTicket](
    GameTicket("game1", Seat(sectorB, 1, 1), Supporter("123", "s10")), GameTicket("game1", Seat(sectorB, 1, 4), Supporter("123", "s12")),
    GameTicket("game2", Seat(sectorB, 1, 2), Supporter("124", "s2")), GameTicket("game1", Seat(sectorB, 1, 1), Supporter("125", "s3")),
    GameTicket("game1", Seat(sectorA, 1, 1), Supporter("123", "s10")), GameTicket("game1", Seat(sectorA, 1, 4), Supporter("123", "s12")),
    GameTicket("game2", Seat(sectorA, 1, 2), Supporter("124", "s2")), GameTicket("game1", Seat(sectorA, 1, 1), Supporter("125", "s3")),
  )
  val emptyDb: STM[Nothing, TSet[GameTicket]] = TSet.empty[GameTicket]

  private def testEnv(db: STM[Nothing,TSet[GameTicket]]) = {
    val dbLayer = db.commit.toLayer >>> Database.live
    dbLayer ++ SectorsRepository.live
  }

  val testReserveSeats: Spec[Any, TestFailure[Any], TestSuccess] = suite("Ticket reservation in non empty db")(
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
          assert(r1.size)(equalTo(2)) &&
          assert(r2.size)(equalTo(2)) &&
          assert(r3.size)(equalTo(2)) &&
          assert(r1.equals(r2))(isFalse) &&
          assert(r1.equals(r3))(isFalse)
      }
      res.provideLayer(testEnv(db))
    },
    testM("many parallel reservation should work"){
      val res = for {
        f <- ZIO.foreach(1 to 20)(_ => SectorsRepository.reserveSeats(4, "B").fork)
        r <- ZIO.foreach(f)(_.join)
      } yield {
        assert(r.flatten.size)(equalTo(20*4))
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
      assertM(res)(equalTo(Seat(sectorB, 1, 5)))
    },
    testM("findFreeSeats Fails when capacity is incorrect"){
      for {
        res <- SectorsRepository.findFreeSeats(26, sectorA.name).commit.provideLayer(testEnv(db)).flip
      } yield assert(res)(equalTo("No free seats available in this Sector"))
    }
  )
  val testReserveSeatsWithEmptyDB: Spec[Any, TestFailure[Any], TestSuccess] = suite("Ticket reservation in empty db")(
    testM("reserve seats should retrun seat seq on random start") {
      val res = SectorsRepository.reserveSeats(5, sectorA.name).provideLayer(testEnv(emptyDb))
      assertM(res.map(_.size))(equalTo(5))
    })

}

