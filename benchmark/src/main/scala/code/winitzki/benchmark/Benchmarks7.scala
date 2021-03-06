package code.winitzki.benchmark

import java.time.LocalDateTime
import code.winitzki.benchmark.Common._
import code.winitzki.jc._
import code.winitzki.jc.JoinRun._

object Benchmarks7 {

  /// Concurrent decrement of `n` counters, each going from `count` to 0 concurrently.

  /// create `n` asynchronous counters, initialize each to `count`, then decrement `count*n` times, until all counters are zero.
  /// collect the zero-counter events, make sure there are `n` of them, then fire an `all_done` event that yields the benchmark time.
  val counters = 20

  def benchmark7(count: Int, threads: Int = 2): Long = {

    println(s"Creating $counters concurrent counters, each going from $count to 0")
    val done = ja[Unit]("done")
    val all_done = ja[Int]("all_done")
    val f = js[LocalDateTime,Long]("f")

    val tp = new JReactionPool(threads)

    join(
      run { case all_done(0) + f(tInit, r) =>
        r(elapsed(tInit))
      },

      run { case all_done(x) + done(_) if x > 0 => all_done(x-1) }
    )
//    done.setLogLevel(2)
    val initialTime = LocalDateTime.now
    all_done(counters)

    val d = make_counters(done, counters, count, tp)
//    d.setLogLevel(2)
    (1 to (count*counters)).foreach{ _ => d() }

    var result = f(initialTime)
    tp.shutdownNow()
    result
  }

  // this deadlocks whenever `count` * `counters` becomes large.
  def benchmark8(count: Int, threads: Int = 2): Long = {

    println(s"Creating $counters concurrent counters, each going from $count to 0")

    object j8 extends Join {
      object done extends AsyName[Unit]
      object all_done extends AsyName[Int]
      object f extends SynName[LocalDateTime, Long]

      join {
        case all_done(0) and f(tInit) =>
          f.reply(elapsed(tInit))
        case all_done(x) and done(_) if x > 0 => all_done(x-1)
      }

    }

    val initialTime = LocalDateTime.now
    j8.all_done(count)
    val d = make_counters8a(j8.done, counters, count, threads)
    (1 to (count*counters)).foreach{ _ => d() }
    j8.f(initialTime)
  }

  def make_counters(done: JA[Unit], counters: Int, init: Int, tp: JReactionPool) = {
    val c = ja[Int]("c")
    val d = ja[Unit]("d")

    join(
      tp{ case c(0) => done() },
      tp{ case c(n) + d(_) if n > 0 => c(n - 1) }
    )
    (1 to counters).foreach(_ => c(init))
    // We return just one molecule.
    d
  }

  def make_counters8a(done: AsyName[Unit], counters: Int, init: Int, threads: Int): AsyName[Unit] = {
    object j8a extends Join {
      object c extends AsyName[Int]
      object d extends AsyName[Unit]

      join {
        case c(0) => done()
        case c(n) and d(_) if n > 0 => c(n-1)
      }

    }
    (1 to counters).foreach(_ => j8a.c(init))
    // We return just one molecule.
    j8a.d
  }

}