package cache

import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}


// A stress-test, to run and observe GC logs
class CacheBenchmark {
  import CacheBenchmark._

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def fillCapacityReference(ss: SharedState, cs: HashMapState, bh: Blackhole): Unit = {
    cs.cache.put(ss.rand.nextLong, ss.rand.nextDouble)
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def fillCapacity(ss: SharedState, cs: CacheState, bh: Blackhole): Unit = {
    cs.cache.update(ss.rand.nextLong, ss.rand.nextDouble)
  }
}

object CacheBenchmark {

  @State(Scope.Thread)
  class SharedState {
    val rand = new Random()
  }

  @State(Scope.Thread)
  class CacheState {
    val cache = new Cache[Long, Double](1000)

    @Setup(Level.Iteration)
    def clear(): Unit = cache.clear()
  }

  @State(Scope.Thread)
  class HashMapState {
    val cache = new ConcurrentHashMap[Long, Double](1000)

    @Setup(Level.Iteration)
    def clear(): Unit = cache.clear()
  }
}
