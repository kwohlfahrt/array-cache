package cache

import scala.util.Random

import org.scalatest._
import flatspec._
import matchers.should._

class RingSpec extends  AnyFlatSpec with Matchers {
  val ttl = 10;
  "A Ring" should "store as many values as it has capacity for" in {
    val capacity = 8
    val rand = new Random()
    val ring = new Ring[Long, Double](capacity)
    val items = (0 to (capacity - 1)).map(_ => rand.nextLong -> rand.nextDouble)
    val indices = items.map(ring.push(_).intValue)
    indices.zip(items).foreach { case (i, item) => ring(i) shouldEqual (Some(item)) }
  }

  "A Ring" should "overwrite values when full" in {
    val capacity = 8
    val rand = new Random()
    val ring = new Ring[Long, Double](capacity)
    val items = (0 to (capacity - 1)).map(_ => rand.nextLong -> rand.nextDouble)
    items.foreach(ring.push(_))
    val item = (rand.nextLong, rand.nextDouble)
    val index = ring.push(item).intValue
    ring(index) shouldEqual Some(item)
  }
}
