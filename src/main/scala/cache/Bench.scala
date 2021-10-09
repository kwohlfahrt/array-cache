package cache

import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}


// A stress-test, to run and observe GC logs
class CacheBenchmark {
  import CacheBenchmark._

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def fillReference(ss: SharedState, cs: HashMapState, bh: Blackhole): Unit = {
    cs.cache.put(ss.rand.nextLong, ss.rand.nextDouble)
  }

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def readReference(ss: SharedState, cs: HashMapState, bh: Blackhole): Unit = {
    bh.consume(cs.cache.get(ss.rand.nextLong))
  }

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def fill(ss: SharedState, cs: CacheState, bh: Blackhole): Unit = {
    cs.cache.update(ss.rand.nextLong, ss.rand.nextDouble)
  }

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def read(ss: SharedState, cs: CacheState, bh: Blackhole): Unit = {
    bh.consume(cs.cache(ss.rand.nextLong))
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
    val fullCache = new Cache[Long, Double](1000)

    @Setup(Level.Iteration)
    def clear(): Unit = cache.clear()

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      fullCache.update(ss.rand.nextLong % 2000, ss.rand.nextDouble)
    }
  }

  @State(Scope.Thread)
  class HashMapState {
    val cache = new ConcurrentHashMap[Long, Double](1000)
    val fullCache = new ConcurrentHashMap[Long, Double](1000)

    @Setup(Level.Iteration)
    def clear(): Unit = cache.clear()

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      fullCache.put(ss.rand.nextLong % 2000, ss.rand.nextDouble)
    }
  }
}
