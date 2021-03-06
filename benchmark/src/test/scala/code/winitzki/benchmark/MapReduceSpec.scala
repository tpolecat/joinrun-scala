package code.winitzki.benchmark

import java.time.LocalDateTime

import code.winitzki.benchmark.Common._
import code.winitzki.jc.JoinRun._
import org.scalatest.{FlatSpec, Matchers}

class MapReduceSpec extends FlatSpec with Matchers {

  it should "perform a map/reduce-like computation" in {
    val count = 10

    val initTime = LocalDateTime.now

    val res = ja[List[Int]]
    val r = ja[Int]
    val d = ja[Int]
    val get = js[Unit, List[Int]]

    join(
      &{ case d(n) => r(n*2) },
      &{ case res(list) + r(s) => res(s::list) },
      &{ case get(_, reply) + res(list) => reply(list) }
    )

    (1 to count).foreach(x => d(x))
    val expectedResult = (1 to count).map(_ * 2)
    res(Nil)

    waitSome()
    get().toSet shouldEqual expectedResult.toSet

    println(s"map/reduce test with n=$count took ${elapsed(initTime)} ms")
  }

}
