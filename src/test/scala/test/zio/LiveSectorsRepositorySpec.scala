package test.zio

import _root_.test.zio.domain.model.{GameTicket, Seat, Sector, Supporter}
import _root_.test.zio.domain.{Database, Tickets}
import zio.{ZIO, clock}
import zio.stm.{STM, TSet}
import zio.test.Assertion._
import zio.test._


object LiveTicketsRepositorySpec extends DefaultRunnableSpec {
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("LiveTicketsRepositorySpec")(testReserveSeats, testReserveSeatsWithEmptyDB)

  val sectorA: Sector = Sector("A")
  val sectorB: Sector = Sector("B")
  val game            = "game1"
  val singleSupporter = Supporter(125, "s3")

  val db: STM[Nothing, TSet[GameTicket]] = TSet.make[GameTicket](
    GameTicket("game1", Seat(sectorB, 1, 1), Supporter(123, "s10")), GameTicket("game1", Seat(sectorB, 1, 4), Supporter(123, "s12")),
    GameTicket("game2", Seat(sectorB, 1, 2), Supporter(124, "s2")), GameTicket("game1", Seat(sectorB, 1, 1), Supporter(125, "s3")),
    GameTicket("game1", Seat(sectorA, 1, 1), Supporter(123, "s10")), GameTicket("game1", Seat(sectorA, 1, 4), Supporter(123, "s12")),
    GameTicket("game2", Seat(sectorA, 1, 2), Supporter(124, "s2")), GameTicket("game1", Seat(sectorA, 1, 1), Supporter(125, "s3")),
  )
  val emptyDb: STM[Nothing, TSet[GameTicket]] = TSet.empty[GameTicket]

  private def testEnv(db: STM[Nothing,TSet[GameTicket]]) = {
    val dbLayer = db.commit.toLayer >>> Database.live
    dbLayer ++ Tickets.live ++ clock.Clock.live 
  }

  val testReserveSeats: Spec[Any, TestFailure[Any], TestSuccess] = suite("Ticket reservation in non empty db")(
    testM("reserveSeats should return seats seq that can be purchased") {
      for {
        res <- Tickets.reserveSeats(1, 2, game).provideLayer(testEnv(db))
      } yield {
        assert(res.size)(equalTo(2))
      }
    },
    testM("parallel reserveSeats should return seats seq that can be purchased") {
      val res = for {
        f1 <- ZIO.foreach(1 to 3)(i=>Tickets.reserveSeats(i, 2, game).fork)
        r  <- ZIO.foreach(f1)(_.join)
        f1 <- Tickets.reserveSeats(1, 2, game).fork
        f2 <- Tickets.reserveSeats(2, 2, game).fork
        f3 <- Tickets.reserveSeats(3, 2, game).fork
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
    testM("more parallel reserveSeats should return seats seq that can be purchased") {
      val res = for {
        f1 <- ZIO.foreach(1 to 20)(i=>Tickets.reserveSeats(i, 2, game).fork)
        r  <- ZIO.foreach(f1)(_.join)
      } yield {
        assert(r.flatten.size)(equalTo(40))
      }
      res.provideLayer(testEnv(db))
    },
    testM("many parallel reservation should work"){
      val res = for {
        f <- ZIO.foreach(1 to 20)(_ => Tickets.reserveSeats(1, 4, game).fork)
        r <- ZIO.foreach(f)(_.join)
      } yield {
        assert(r.flatten.size)(equalTo(20*4))
      }
      res.provideLayer(testEnv(db))
    },
    testM("parallel reservation should work for given sequence"){
      val res = for {
        f1  <- Tickets.reserveSeats(Seq(Seat(sectorA, 9, 1),Seat(sectorA, 9, 2),Seat(sectorA, 9, 3)),game,singleSupporter).fork
        f2  <- Tickets.reserveSeats(Seq(Seat(sectorA, 8, 1),Seat(sectorA, 8, 2),Seat(sectorA, 8, 3)),game,singleSupporter).fork
        f3  <- Tickets.reserveSeats(Seq(Seat(sectorA, 7, 1),Seat(sectorA, 7, 2),Seat(sectorA, 7, 3)),game,singleSupporter).fork
        r1  <- f1.join
        r2  <- f2.join
        r3  <- f3.join
      } yield {
        assert(r1)(equalTo(List(GameTicket(game, Seat(sectorA, 9, 1),singleSupporter), GameTicket(game, Seat(sectorA, 9, 2),singleSupporter), GameTicket(game, Seat(sectorA, 9, 3),singleSupporter)))) &&
        assert(r2)(equalTo(List(GameTicket(game, Seat(sectorA, 8, 1),singleSupporter), GameTicket(game, Seat(sectorA, 8, 2),singleSupporter), GameTicket(game, Seat(sectorA, 8, 3),singleSupporter)))) &&
        assert(r3)(equalTo(List(GameTicket(game, Seat(sectorA, 7, 1),singleSupporter), GameTicket(game, Seat(sectorA, 7, 2),singleSupporter), GameTicket(game, Seat(sectorA, 7, 3),singleSupporter))))
      }
      res.provideLayer(testEnv(db))
    },
    testM("reserveSeats to many seats should fail"){
      for {
        res <- Tickets.reserveSeats(1, 26, game).provideLayer(testEnv(db)).flip
      } yield assert(res)(equalTo("Incorrect capacity"))
    }
  )
  val testReserveSeatsWithEmptyDB: Spec[Any, TestFailure[Any], TestSuccess] = suite("Ticket reservation in empty db")(
    testM("reserve seats should retrun seat seq on random start") {
      val res = Tickets.reserveSeats(1, 5, game).provideLayer(testEnv(emptyDb))
      assertM(res.map(_.size))(equalTo(5))
    })

}

