package cache

import scala.util.Random
import scala.concurrent.{Future, ExecutionContext}
import java.util.concurrent.Executors

import org.scalatest._
import flatspec._
import matchers.should._

class RingSpec extends  AsyncFlatSpec with Matchers with Inspectors {
  val ttl = 10;

  "A Ring" should "store as many values as it has capacity for" in {
    val capacity = 8
    val rand = new Random(4)
    val ring = new Ring[Long, Double](capacity)
    val items = (0 until capacity).map(_ => rand.nextLong -> rand.nextDouble)
    val indices = items.map(ring.push(_))
    indices.flatMap(ring(_)) shouldEqual items
  }

  "A Ring" should "overwrite values when full" in {
    val capacity = 8
    val rand = new Random(4)
    val ring = new Ring[Long, Double](capacity)
    val items = (0 until capacity).map(_ => rand.nextLong -> rand.nextDouble)
    val indices = items.map(ring.push(_))
    ring(indices(0)) should contain (items(0))
    val item = (rand.nextLong, rand.nextDouble)
    val index = ring.push(item)
    ring(index) should contain (item)
    ring(indices(0)) shouldBe empty
  }

  "A Ring" should "store and retrieve values reliably from multiple threads" in {
    val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    val capacity = 100
    val ring = new Ring[Long, Double](capacity)
    val rand = new Random(4)
    val items = (0 until capacity).map(_ => rand.nextLong -> rand.nextDouble)
    val indices = items.map(i => Future { ring.push(i) } (ec))
    forAll(indices.zip(items)) {
      case (idx, item) => idx map { ring(_) should contain (item) }
    }
  }

  "A Ring" should "invalidate items from multiple threads" in {
    val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    val capacity = 100
    val ring = new Ring[Long, Double](capacity)
    val rand = new Random(4)
    val items = (0 until (2 * capacity)).map(_ => rand.nextLong -> rand.nextDouble)
    val indices = items.map(i => Future { ring.push(i) } (ec))
    forAll(indices.zip(items)) {
      case (idx, item) => idx map { ring(_) should (be (empty) or contain (item)) }
    }
  }
}
