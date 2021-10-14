package cache

import scala.util.Random
import scala.concurrent.{Future, ExecutionContext}
import java.util.concurrent.Executors

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures._
import flatspec._
import matchers.should._

class RingSpec extends AsyncFlatSpec with Matchers with Inspectors {
  val ttl = 10;

  "A Ring" should "store as many values as it has capacity for" in {
    val capacity = 8
    val rand = new Random(4)
    val ring = new Ring[Long, Double](capacity)
    val items = (0 until capacity).map(_ => rand.nextLong -> Array(rand.nextDouble))
    val indices = items.map { case (k, v) => ring.push(k, v)}
    forAll(indices.zip(items)) {
      case (idx, (k, v)) => ring(idx, k) should contain (v)
    }
  }

  "A Ring" should "return copies of input data" in {
    val capacity = 8
    val rand = new Random(4)
    val ring = new Ring[Long, Double](capacity)
    val (k, v) = rand.nextLong -> Array(1.0)
    val index = ring.push(k, v)
    val Some(v2) = ring(index, k)
    v2(0) = 5.0
    ring(index, k) should contain (Array(1.0))
  }

  it should "overwrite values when full" in {
    val capacity = 8
    val rand = new Random(4)
    val ring = new Ring[Long, Double](capacity)
    val items = (0 until capacity).map(_ => rand.nextLong -> Array(rand.nextDouble))
    val indices = items.map { case (k, v) => ring.push(k, v)}
    ring(indices(0), items(0)._1) should contain (items(0)._2)
    val (k, v) = (rand.nextLong, Array(rand.nextDouble))
    val index = ring.push(k, v)
    ring(index, k) should contain (v)
    ring(indices(0), items(0)._1) shouldBe empty
  }

  it should "store and retrieve values reliably from multiple threads" in {
    val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    val capacity = 100
    val ring = new Ring[Long, Double](capacity)
    val rand = new Random(4)
    val items = (0 until capacity).map(_ => rand.nextLong -> Array(rand.nextDouble))
    val indices = items.map { case (k, v) => Future { ring.push(k, v) } (ec) }
    forAll(indices.zip(items)) {
      case (f, (k, v)) => whenReady(f) { idx => ring(idx, k) should contain (v) }
    }
  }

  it should "invalidate items from multiple threads" in {
    val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    val capacity = 100
    val ring = new Ring[Long, Double](capacity)
    val rand = new Random(4)
    val items = (0 until (2 * capacity)).map(_ => rand.nextLong -> Array(rand.nextDouble))
    val indices = items.map { case (k, v) => Future { ring.push(k, v) } (ec) }
    forAll(indices.zip(items)) {
      case (f, (k, v)) => whenReady(f) { idx => ring(idx, k) should (be (empty) or contain (v)) }
    }
  }
}
