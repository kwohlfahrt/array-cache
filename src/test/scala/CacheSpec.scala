package cache

import scala.util.Random

import org.scalatest._
import flatspec._
import matchers.should._

class CacheSpec extends  AnyFlatSpec with Matchers {
  val ttl = 100;

  "A Cache" should "store values" in {
    val cache = new Cache[Long, Double](ttl, clock=() => ttl + 1)
    cache.update(1, 2.0)
    cache(1) should equal (Some(2.0))
  }

  "A Cache" should "grow when values are inserted" in {
    val cache = new Cache[Long, Double](ttl, clock=() => ttl + 1)
    cache.capacity should equal (1)
    for (i <- 0 to 10) {
      cache.update(i, 2.0)
    }
    cache.capacity should be > 1
  }

  "A Cache" should "not grow too large" in {
    val cache = new Cache[Long, Double](ttl, clock=() => ttl + 1)
    cache.capacity should equal (1)
    val rand = new Random()
    val items = (0 to 1000).map(_ => rand.nextLong -> rand.nextDouble)
    items.foreach { case (k, v) => cache.update(k, v) }
    cache.capacity should be < 2000
  }

  "A Cache" should "store most values" in {
    val cache = new Cache[Long, Double](ttl, clock=() => ttl + 1)
    cache.capacity should equal (1)
    val rand = new Random()
    val items = (0 to ttl).map(_ => rand.nextLong -> rand.nextDouble)
    items.foreach { case (k, v) => cache.update(k, v) }
    val hits = items.count { case (k, v) => cache(k) == Some(v) } 
    hits should be > 950
  }

  "A Cache" should "Let values expire" in {
    var time = ttl + 1
    val cache = new Cache[Long, Double](ttl, clock=() => time)
    cache.update(1, 2.0)
    cache(1) should equal (Some(2.0))
    time += ttl + 1
    cache(1) should equal (None)
  }

  "A Cache" should "Not count expired values as collisions" in {
    var time = ttl + 1
    val cache = new Cache[Long, Double](ttl, clock=() => time)
    cache.capacity should equal (1)
    for (i <- 0 to 1000) {
      cache.update(i, 2.0)
      time += ttl + 1
    }
    cache.capacity should equal (1)
  }
}
