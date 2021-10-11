package cache.bench

import cache._
import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit


class CacheBenchmark {
  import CacheBenchmark._

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def fill(ss: SharedState, cs: EmptyCache, bh: Blackhole): Unit = {
    cs.cache.update(ss.rand.nextLong % (2 * N), ss.rand.nextDouble)
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def read(ss: SharedState, cs: FullCache, bh: Blackhole): Unit = {
    bh.consume(cs.cache(ss.rand.nextLong % (2 * N)))
  }

  @Benchmark
  @Group("mixed")
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def fillMixed(ss: SharedState, cs: MixedCache, bh: Blackhole): Unit = {
    cs.cache.update(ss.rand.nextLong % (2 * N), ss.rand.nextDouble)
  }

  @Benchmark
  @Group("mixed")
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def readMixed(ss: SharedState, cs: MixedCache, bh: Blackhole): Unit = {
    bh.consume(cs.cache(ss.rand.nextLong % (2 * N)))
  }
}

object CacheBenchmark {
  var N: Int = 1000

  @State(Scope.Thread)
  class SharedState {
    val rand = new Random(4)
  }

  @State(Scope.Benchmark)
  class EmptyCache {
    val cache = new Cache[Long, Double](N)

    @Setup(Level.Iteration)
    def clear(): Unit = cache.clear()
  }

  @State(Scope.Benchmark)
  class FullCache {
    val cache = new Cache[Long, Double](N)

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      for (_ <- 0 to N) {
        cache.update(ss.rand.nextLong % (2 * N), ss.rand.nextDouble)
      }
    }
  }

  @State(Scope.Group)
  class MixedCache {
    val cache = new Cache[Long, Double](N)

    @Setup(Level.Iteration)
    def clear(): Unit = cache.clear()
  }
}
