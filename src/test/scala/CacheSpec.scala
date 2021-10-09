package cache

import scala.util.Random
import scala.concurrent.{Future, ExecutionContext}
import java.util.concurrent.Executors

import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.concurrent.ScalaFutures._
import flatspec._
import matchers.should._

class CacheSpec extends AsyncFlatSpec with Matchers with Inspectors {
  "A Cache" should "store values" in {
    val cache = new Cache[Long, Double](1)
    cache.update(1, 2.0)
    cache(1) should contain (2.0)
  }

  it should "return None for missing values" in {
    val cache = new Cache[Long, Double]()
    cache(1) shouldBe empty
  }

  it should "store most values" in {
    val capacity = 100
    val cache = new Cache[Long, Double](capacity)
    val rand = new Random(4)
    val items = (0 to capacity).map(_ => rand.nextLong -> rand.nextDouble)
    items.foreach { case (k, v) => cache.update(k, v) }
    forAll(items) {
      case (k, v) => cache(k) should (be (empty) or contain (v))
    }
    forAtMost((capacity * 0.25).toInt, items) {
      case (k, _) => cache(k) shouldBe empty
    }
  }

  it should "store and retrieve values reliably from multiple threads" in {
    val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    val capacity = 100
    val cache = new Cache[Long, Double](capacity)
    val rand = new Random(4)
    val items = (0 until capacity).map(_ => rand.nextLong -> rand.nextDouble)
    val keys = items.map { case (k, v) => Future { cache.update(k, v) } (ec)}
    forAtMost((capacity * 0.25).toInt, keys.zip(items)) {
      case (f, (k, v)) => whenReady(f) { _ => cache(k) shouldBe empty }
    }
    forAll(keys.zip(items)) {
      case (f, (k, v)) => whenReady(f) { _ => cache(k) should (be (empty) or contain (v)) }
    }
  }
}

class CacheProps extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  trait FullCache {
    val cache = new Cache[Long, Double](1000)
  }

  "A Cache" should "retrieve all sorts of values" in new FullCache {
    forAll { (i: Long) =>
      cache(i) should be (empty)
    }
  }
}
