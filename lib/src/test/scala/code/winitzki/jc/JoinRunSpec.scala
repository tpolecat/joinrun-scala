package code.winitzki.jc

import JoinRun._
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.concurrent.Waiters.Waiter
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}

class JoinRunSpec extends FlatSpec with Matchers with TimeLimitedTests {

  val timeLimit = Span(500, Millis)

  val warmupTimeMs = 50

  def waitSome(): Unit = Thread.sleep(warmupTimeMs)


  it should "define a reaction with correct inputs" in {
    val a = ja[Unit]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    a.joinDef.isEmpty shouldEqual true
    a.toString shouldEqual "a"

    join(&{ case a(_) + b(_) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nNo molecules"

    a()
    a()
    b()
    waitSome()
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nMolecules: a() * 2, b()"
  }

  it should "define a reaction with correct inputs with non-default pattern-matching at end of reaction" in {
    val a = ja[Option[Int]]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case b(_) + c(_) + a(Some(x)) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nNo molecules"
  }

  it should "define a reaction with correct inputs with non-default pattern-matching in the middle of reaction" in {
    val a = ja[Option[Int]]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case b(_) + a(Some(x)) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b => ...}\nNo molecules"  // this is the wrong result
    // when the problem is fixed, this test will have to be rewritten
  }

  it should "define a reaction with correct inputs with default pattern-matching in the middle of reaction" in {
    val a = ja[Option[Int]]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case b(_) + a(None) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nNo molecules"
  }

  it should "define a reaction with correct inputs with non-simple default pattern-matching in the middle of reaction" in {
    val a = ja[Seq[Int]]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case b(_) + a(List()) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nNo molecules"
  }


  it should "define a reaction with correct inputs with empty option pattern-matching at start of reaction" in {
    val a = ja[Option[Int]]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case a(None) + b(_) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nNo molecules"
  }

  it should "define a reaction with correct inputs with constant default pattern-matching at start of reaction" in {
    val a = ja[Int]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case a(0) + b(_) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nNo molecules"
  }

  it should "define a reaction with correct inputs with constant non-default pattern-matching at start of reaction" in {
    val a = ja[Int]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case a(1) + b(_) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a => ...}\nNo molecules" // this is the wrong result
    // when the problem is fixed, this test will have to be rewritten
  }

  it should "define a reaction with correct inputs with constant default option pattern-matching at start of reaction" in {
    val a = ja[Option[Int]]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case a(None) + b(_) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nNo molecules"
  }

  it should "define a reaction with correct inputs with constant non-default pattern-matching at end of reaction" in {
    val a = ja[Int]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case b(_) + c(_) + a(1) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a + b + c => ...}\nNo molecules"
  }

  it should "define a reaction with correct inputs with non-default pattern-matching at start of reaction" in {
    val a = ja[Option[Int]]("a")
    val b = ja[Unit]("b")
    val c = ja[Unit]("c")

    join(&{ case a(Some(x)) + b(_) + c(_) => })

    a.joinDef.isEmpty shouldEqual false
    a.joinDef.get.printBag shouldEqual "Join{a => ...}\nNo molecules" // this is the wrong result
    // when the problem is fixed, this test will have to be rewritten
  }

  it should "generate an error message when the inputs are incorrectly inferred from reaction" in {
    val a = ja[Option[Int]]("a")
    val b = ja[Unit]("b")

    join(&{ case a(Some(x)) + b(_) => }) // currently, the limitations in the pattern-macher will cause this
    // to fail to recognize that "b" is an input molecule in this reaction.

    // when the problem is fixed, this test will have to be rewritten

    a(Some(1))
    val thrown = intercept[Exception] {
      b()
    }
    thrown.getMessage shouldEqual "Molecule b does not belong to any join definition"

  }

  it should "start a simple reaction with one input, defining the injector explicitly" in {

    val waiter = new Waiter

    val a = new JA[Unit]

    join( &{ case a(_) => waiter.dismiss() })

    a()
    waiter.await()
  }

  it should "start a simple reaction with one input" in {

    val waiter = new Waiter

    val a = ja[Unit]
    join( &{ case a(_) => waiter.dismiss() })
    a()
    waiter.await()
  }

  it should "start a simple reaction chain" in {

    val waiter = new Waiter

    val a = ja[Unit]("a")
    val b = ja[Unit]("b")
    join( &{ case a(_) => b() }, &{ case b(_) => waiter.dismiss() })

    a()
    waiter.await()
  }

  it should "start a simple reaction chain with two inputs with values" in {

    val waiter = new Waiter

    val a = ja[Int]
    val b = ja[Int]
    val c = ja[Int]
    join( &{ case a(x) + b(y) => c(x+y) }, &{ case c(z) => waiter { z shouldEqual 3 }; waiter.dismiss() })
    a(1)
    b(2)
    waiter.await()
  }

  it should "block for a synchronous molecule" in {

    val a = ja[Unit]
    val f = js[Unit,Int]
    join( &{ case a(_) + f(_, r) => r(3) })
    a()
    a()
    a()
    f() shouldEqual 3
    f() shouldEqual 3
    f() shouldEqual 3
  }

  it should "throw exception when join pattern is nonlinear" in {
    val thrown = intercept[Exception] {
      val a = ja[Unit]("a")
      join( &{ case a(_) + a(_) => () })
      a()
    }
    thrown.getMessage shouldEqual "Nonlinear pattern: a used twice"

  }

  it should "throw exception when join pattern is nonlinear, with blocking molecule" in {
    val thrown = intercept[Exception] {
      val a = js[Unit,Unit]("a")
      join( &{ case a(_,r) + a(_,s) => () })
      a()
    }
    thrown.getMessage shouldEqual "Nonlinear pattern: a/S used twice"
  }

  it should "throw exception when join pattern attempts to redefine a blocking molecule" in {
    val thrown = intercept[Exception] {
      val a = js[Unit,Unit]("a")
      join( &{ case a(_,_) => () })
      join( &{ case a(_,_) => () })
    }
    thrown.getMessage shouldEqual "Molecule a/S cannot be used as input since it was already used in Join{a/S => ...}"
  }

  it should "throw exception when join pattern attempts to redefine a non-blocking molecule" in {
    val thrown = intercept[Exception] {
      val a = ja[Unit]("x")
      val b = ja[Unit]("y")
      join( &{ case a(_) + b(_) => () })
      join( &{ case a(_) => () })
    }
    thrown.getMessage shouldEqual "Molecule x cannot be used as input since it was already used in Join{x + y => ...}"
  }

  it should "throw exception when trying to inject a blocking molecule that has no join" in {
    val thrown = intercept[Exception] {
      val a = ja[Unit]("x")
      a()
    }
    thrown.getMessage shouldEqual "Molecule x does not belong to any join definition"
  }

  it should "fail to start reactions when pattern is not matched" in {

    val a = ja[Int]
    val b = ja[Int]
    val f = js[Unit,Int]

    join( &{ case a(x) + b(0) => a(x+1) }, &{ case a(z) + f(_, r) => r(z) })
    a(1)
    b(2)
    waitSome()
    f() shouldEqual 1
  }

  it should "implement the non-blocking single-access counter" in {
    val c = ja[Int]("c")
    val d = ja[Unit]("decrement")
    val g = js[Unit,Int]("getValue")
    join(
      &{ case c(n) + d(_) => c(n-1) },
      &{ case c(n) + g(_,r) => c(n) + r(n) }
    )
    c(2) + d() + d()
    waitSome()
    g() shouldEqual 0

  }

  it should "use one thread for concurrent computations" in {
    val c = ja[Int]("counter")
    val d = ja[Unit]("decrement")
    val f = ja[Unit]("finished")
    val a = ja[Int]("all_finished")
    val g = js[Unit,Int]("getValue")

    val tp = new JReactionPool(1)

    join(
      &{ case c(x) + d(_) => Thread.sleep(100); c(x-1) + f() } onThreads tp,
      &{ case a(x) + g(_, r) => a(x) + r(x) },
      &{ case f(_) + a(x) => a(x+1) }
    )
    a(0) + c(1) + c(1) + d() + d()
    Thread.sleep(150) // This is less than 200ms, so we have not yet finished the second computation.
    g() shouldEqual 1
    Thread.sleep(150) // Now we should have finished the second computation.
    g() shouldEqual 2

    tp.shutdownNow()
  }

  it should "use two threads for concurrent computations" in {
    val c = ja[Int]("counter")
    val d = ja[Unit]("decrement")
    val f = ja[Unit]("finished")
    val a = ja[Int]("all_finished")
    val g = js[Unit,Int]("getValue")

    val tp = new JReactionPool(2)

    join(
      &{ case c(x) + d(_) => Thread.sleep(100); c(x-1) + f() } onThreads tp,
      &{ case a(x) + g(_, r) => r(x) },
      &{ case f(_) + a(x) => a(x+1) }
    )
    a(0) + c(1) + c(1) + d() + d()
    Thread.sleep(150) // This is less than 200ms, and the test fails unless we use 2 threads concurrently.
    g() shouldEqual 2

    tp.shutdownNow()
  }

  it should "process simple reactions quickly enough" in {
    val n = 2000

    val c = ja[Int]("counter")
    val d = ja[Unit]("decrement")
    val g = js[Unit, Int]("getValue")
    val tp = new JReactionPool(2)
    join(
      & { case c(x) + d(_) => c(x - 1) } onThreads tp,
      & { case c(x) + g(_, r) => c(x) + r(x) }
    )
    c(n)
    (1 to n).foreach { _ => d() }

    Thread.sleep(400)
    g() shouldEqual 0

    tp.shutdownNow()
  }

  it should "complete the task even if processes will crash with fixed probability" in {
    val n = 20

    val probabilityOfCrash = 0.5

    val c = ja[Int]("counter")
    val d = ja[Unit]("decrement")
    val g = js[Unit, Int]("getValue")
    val tp = new JReactionPool(2)

    join(
      & { case c(x) + d(_) =>
        if (scala.util.Random.nextDouble >= probabilityOfCrash) c(x - 1) else throw new Exception("crash! (it's ok, ignore this)")
      } onThreads tp,
      & { case c(x) + g(_, r) => c(x) + r(x) }
    )
    c(n)
    (1 to n).foreach { _ => d() }

    waitSome()
    Thread.sleep(200) // give it some more time to compensate for crashes
    g() shouldEqual 0

    tp.shutdownNow()
  }

  it should "throw exception when a reaction attempts to reply twice" in {
    val c = ja[Int]("c")
    val g = js[Unit,Int]("g")
    join(
      &{ case c(n) + g(_,r) => c(n) + r(n) + r(n+1) }
    )
    c(2)
    waitSome()

    val thrown = intercept[Exception] {
      println(s"got result: ${g()} but should not have printed this!")
    }
    thrown.getMessage shouldEqual "Error: In Join{c + g/S => ...}: Reaction {c + g/S => ...} replied to g/S more than once"
  }

  it should "throw exception when a reaction attempts to reply twice to more than one molecule" in {
    val c = ja[Int]("c")
    val d = ja[Unit]("d")
    val g = js[Unit,Int]("g")
    val g2 = js[Unit,Int]("g2")
    join(
      &{ case d(_) => g2() },
      &{ case c(n) + g(_,r) + g2(_, r2) => c(n) + r(n) + r(n+1) + r2(n) + r2(n+1) }
    )
    c(2) + d()
    waitSome()

    val thrown = intercept[Exception] {
      println(s"got result: ${g()} but should not have printed this!")
    }
    thrown.getMessage shouldEqual "Error: In Join{d => ...; c + g/S + g2/S => ...}: Reaction {c + g/S + g2/S => ...} replied to g/S, g2/S more than once"
  }

  it should "throw exception when a reaction does not reply to one blocking molecule" in {
    val c = ja[Unit]("c")
    val g = js[Unit,Int]("g")
    join(
      &{ case c(_) + g(_,r) => c() }
    )
    c()
    waitSome()

    val thrown = intercept[Exception] {
      println(s"got result: ${g()} but should not have printed this!")
    }
    thrown.getMessage shouldEqual "Error: In Join{c + g/S => ...}: Reaction {c + g/S => ...} finished without replying to g/S"
  }

  it should "throw exception when a reaction does not reply to two blocking molecules)" in {
    val c = ja[Unit]("c")
    val d = ja[Unit]("d")
    val g = js[Unit,Int]("g")
    val g2 = js[Unit,Int]("g2")
    val tp = new JReactionPool(2)
    join(
      &{ case d(_) => g2() } onThreads tp,
      &{ case c(_) + g(_,_) + g2(_,_) => c() }
    )
    c() + d()
    waitSome()

    val thrown = intercept[Exception] {
      println(s"got result2: ${g()} but should not have printed this!")
    }
    thrown.getMessage shouldEqual "Error: In Join{d => ...; c + g/S + g2/S => ...}: Reaction {c + g/S + g2/S => ...} finished without replying to g/S, g2/S"

    tp.shutdownNow()
  }

  it should "throw exception when a reaction does not reply to one blocking molecule but does reply to another" in {
    val c = ja[Unit]("c")
    val d = ja[Unit]("d")
    val g = js[Unit,Int]("g")
    val g2 = js[Unit,Int]("g2")
    val tp = new JReactionPool(2)
    join(
      &{ case d(_) => g2() } onThreads tp,
      &{ case c(_) + g(_,r) + g2(_,_) => c() + r(0) }
    )
    c() + d()
    waitSome()

    val thrown = intercept[Exception] {
      println(s"got result: ${g()} but should not have printed this!")
    }
    thrown.getMessage shouldEqual "Error: In Join{d => ...; c + g/S + g2/S => ...}: Reaction {c + g/S + g2/S => ...} finished without replying to g2/S"

    tp.shutdownNow()
  }

  it should "not produce deadlock when two blocking molecules are injected from different threads" in {
    val c = ja[Unit]("c")
    val d = ja[Unit]("d")
    val e = ja[Int]("e")
    val f = ja[Unit]("f")
    val g = js[Unit,Int]("g")
    val g2 = js[Unit,Int]("g2")
    val h = js[Unit,Int]("h")
    val tp = new JReactionPool(2)
    join(
      &{ case c(_) => e(g2()) } onThreads tp, // e(0) should be injected now
      &{ case d(_) + g(_,r) + g2(_,r2) => r(0) + r2(0) } onThreads tp,
      &{ case e(x) + h(_,r) =>  r(x) }
    )
    c()+d()
    waitSome()
    g() shouldEqual 0
    // now we should also have e(0)
    h() shouldEqual 0

    tp.shutdownNow()

  }

  it should "produce deadlock when two blocking molecules are injected from the same thread" in {
    val c = ja[Unit]("c")
    val d = ja[Unit]("d")
    val e = ja[Int]("e")
    val f = ja[Unit]("f")
    val g = js[Unit,Int]("g")
    val g2 = js[Unit,Int]("g2")
    val h = js[Unit,Int]("h")
    val tp = new JReactionPool(2)
    join(
      &{ case c(_) => val x = g() + g2(); e(x) } onThreads tp, // e(0) should never be injected because this thread is deadlocked
      &{ case d(_) + g(_,r) + g2(_,r2) => r(0) + r2(0) } onThreads tp,
      &{ case e(x) + h(_,r) =>  r(x) },
      &{ case d(_) + f(_) => e(2) },
      &{ case f(_) + e(_) => e(1) }
    )
    d()
    waitSome()
    c()
    waitSome()
    // if e(0) exists now, it will react with f() and produce e(1)
    f()
    waitSome()
    // if e(0) did not appear, f() is still available and will now react with d and produce e(2)
    d()
    waitSome()
    h() shouldEqual 2

    tp.shutdownNow()
  }

}
