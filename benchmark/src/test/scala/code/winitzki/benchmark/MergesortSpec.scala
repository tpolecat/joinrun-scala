package code.winitzki.benchmark

import java.time.LocalDateTime

import Common._
import code.winitzki.jc.JReactionPool
import code.winitzki.jc.JoinRun._
import org.scalatest.{FlatSpec, Matchers}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.ClassTag

class MergesortSpec extends FlatSpec with Matchers {

  // auxiliary functions for merge-sort tests

  // this object is not used now
  object amCounter {
    var c:Int = 0
    def inc(): Unit = {
      synchronized {
        c += 1
      }
    }
  }

  def arrayMerge[T : Ordering : ClassTag](arr1: Array[T], arr2: Array[T]): Array[T] = {
    val id = amCounter.c
    //      amCounter.inc() // avoid this for now - this is a debugging tool
    val wantToLog = false // (arr1.length > 20000 && arr1.length < 41000)
    if (wantToLog) println(s"${System.currentTimeMillis} start merging #$id")

    val result = new mutable.ArraySeq[T](arr1.length + arr2.length) // just to allocate space

    def isLess(x: T, y: T) = implicitly[Ordering[T]].compare(x,y) < 0

    // will now modify result
    @tailrec
    def mergeRec(i1 : Int, i2: Int, i: Int): Unit = {
      if (i1 == arr1.length && i2 == arr2.length) ()
      else {
        val (x, newI1, newI2) = if (i1 < arr1.length && (i2 == arr2.length || isLess(arr1(i1), arr2(i2))))
          (arr1(i1), i1+1, i2) else (arr2(i2), i1, i2+1)
        result(i) = x
        mergeRec(newI1, newI2, i+1)
      }
    }
    mergeRec(0,0,0)
    if (wantToLog) println(s"${System.currentTimeMillis} finished merging #$id")
    result.toArray
  }

  def performMergeSort[T : Ordering : ClassTag](array: Array[T], threads: Int = 8): Array[T] = {

    val finalResult = ja[Array[T]]
    val getFinalResult = js[Unit, Array[T]]

    join(
      &{ case finalResult(arr) + getFinalResult(_, r) => r(arr) }
    )

    // recursive molecule that will define the reactions at one level

    val mergesort = new JA[(Array[T], JA[Array[T]])]

    val tp = new JReactionPool(threads)
    join(
      tp{
        case mergesort((arr, resultToYield)) =>
          if (arr.length <= 1) resultToYield(arr)
          else {
            val (part1, part2) = arr.splitAt(arr.length/2)
            // "sorted1" and "sorted2" will be the sorted results from lower level
            val sorted1 = new JA[Array[T]]
            val sorted2 = new JA[Array[T]]
            join(
              tp{ case sorted1(x) + sorted2(y) =>
                resultToYield(arrayMerge(x,y)) }
            )(tp, defaultJoinPool)

            // inject lower-level mergesort
            mergesort(part1, sorted1) + mergesort(part2, sorted2)
          }
      }
    )(tp, defaultJoinPool)
    // sort our array at top level
    mergesort((array, finalResult))

    val result = getFinalResult()
    tp.shutdownNow()
    result
  }

  it should "merge arrays correctly" in {
    arrayMerge(Array(1,2,5), Array(3,6)) shouldEqual Array(1,2,3,5,6)
  }

  it should "sort an array using concurrent merge-sort correctly with one thread" in {

    val count = 10
    val threads = 1

    val arr = Array.fill[Int](count)(scala.util.Random.nextInt(count))
    val expectedResult = arr.sorted

    performMergeSort(arr, threads) shouldEqual expectedResult
  }

  it should "sort an array using concurrent merge-sort correctly with many threads" in {

    val count = 10
    val threads = 8

    val arr = Array.fill[Int](count)(scala.util.Random.nextInt(count))
    val expectedResult = arr.sorted

    performMergeSort(arr, threads) shouldEqual expectedResult
  }

  it should "sort an array using concurrent merge-sort more quickly with many threads than with one thread" in {

    val count = 100000 // 1000000
    val threads = 8 // typical thread utilization at 600%

    val arr = Array.fill[Int](count)(scala.util.Random.nextInt(count))

    val result = timeWithPriming{ performMergeSort(arr, threads)}
    println(s"concurrent merge-sort test with count=$count and $threads threads took $result ms")

    val result1 = timeWithPriming{ performMergeSort(arr, 1)}
    println(s"concurrent merge-sort test with count=$count and 1 threads took $result1 ms")
  }

}
