package cache

import scala.util.Random

import org.scalatest._
import flatspec._
import matchers.should._

class CacheSpec extends  AnyFlatSpec with Matchers {
  "A Cache" should "store values" in {
    val cache = new Cache[Long, Double](1)
    cache.update(1, 2.0)
    cache(1) should equal (Some(2.0))
  }

  "A Cache" should "return None for missing values" in {
    val cache = new Cache[Long, Double]()
    cache(1) should equal (None)
  }

  "A Cache" should "store most values" in {
    val capacity = 100
    val cache = new Cache[Long, Double](capacity=capacity)
    val rand = new Random(4)
    val items = (0 to capacity).map(_ => rand.nextLong -> rand.nextDouble)
    items.foreach { case (k, v) => cache.update(k, v) }
    val hits = items.count { case (k, v) => cache(k) == Some(v) } 
    hits should be > ((capacity * 0.8).toInt)
  }
}
