package cache

import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.ConcurrentHashMap


// A stress-test, to run and observe GC logs
class CacheBenchmark {
  @Benchmark
  def fillCapacityReference(bh: Blackhole): Unit = {
    val rand = new Random()
    val cache = new ConcurrentHashMap[Long, Double](1000)
    val pairs = (0 to 1000).map(_ => (rand.nextLong, rand.nextDouble))
    for ((k, v) <- pairs) {
      cache.put(k, v)
    }
    for ((k, _) <- pairs) bh.consume(cache.get(k))
  }

  @Benchmark
  def fillCapacity(bh: Blackhole): Unit = {
    val rand = new Random()
    val cache = new Cache[Long, Double](1000)
    val pairs = (0 to 1000).map(_ => (rand.nextLong, rand.nextDouble))
    for ((k, v) <- pairs) {
      cache.update(k, v)
    }
    for ((k, _) <- pairs) bh.consume(cache(k))
  }
}
