package cache

import scala.util.Random

import org.scalatest._
import flatspec._
import matchers.should._

class CacheSpec extends AnyFlatSpec with Matchers with Inspectors {
  "A Cache" should "store values" in {
    val cache = new Cache[Long, Double](1)
    cache.update(1, 2.0)
    cache(1) should contain (2.0)
  }

  "A Cache" should "return None for missing values" in {
    val cache = new Cache[Long, Double]()
    cache(1) shouldBe empty
  }

  "A Cache" should "store most values" in {
    val capacity = 100
    val cache = new Cache[Long, Double](capacity=capacity)
    val rand = new Random(4)
    val items = (0 to capacity).map(_ => rand.nextLong -> rand.nextDouble)
    items.foreach { case (k, v) => cache.update(k, v) }
    forAll(items) {
      case (k, v) => cache(k) should (be (empty) or contain (v))
    }

    forAtMost((capacity * 0.2).toInt, items) {
      case (k, _) => cache(k) shouldBe empty
    }
  }
}
