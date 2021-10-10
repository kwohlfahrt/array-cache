package cache.bench

import cache._
import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit


class CacheBenchmark {
  import CacheBenchmark._

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def fill(ss: SharedState, cs: CacheState, bh: Blackhole): Unit = {
    cs.cache.update(ss.rand.nextLong % 2000, ss.rand.nextDouble)
  }

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def read(ss: SharedState, cs: CacheState, bh: Blackhole): Unit = {
    bh.consume(cs.fullCache(ss.rand.nextLong % 2000))
  }

  @Benchmark
  @Threads(6)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def mixed(ss: SharedState, cs: CacheState, bh: Blackhole): Unit = {
    cs.cache.update(ss.rand.nextLong % 2000, ss.rand.nextDouble)
    bh.consume(cs.cache(ss.rand.nextLong % 2000))
  }
}

object CacheBenchmark {
  @State(Scope.Thread)
  class SharedState {
    val rand = new Random(4)
  }

  @State(Scope.Thread)
  class CacheState {
    val cache = new Cache[Long, Double](1000)
    val fullCache = new Cache[Long, Double](1000)

    @Setup(Level.Iteration)
    def clear(): Unit = cache.clear()

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      for (_ <- 0 to 1000) {
        fullCache.update(ss.rand.nextLong % 2000, ss.rand.nextDouble)
      }
    }
  }
}
