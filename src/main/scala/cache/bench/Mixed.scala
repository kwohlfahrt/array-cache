package cache.bench

import cache._
import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}


@OutputTimeUnit(TimeUnit.MICROSECONDS)
class MixedBenchmark {
  import MixedBenchmark._

  @Benchmark
  @Group("ref")
  def writeRef(ss: SharedState, cs: EmptyConcurrentHashMap): Unit = {
    cs.cache.put(ss.rand.nextLong % (2 * ss.N), Array(ss.rand.nextDouble))
  }

  @Benchmark
  @Group("ref")
  def readRef(ss: SharedState, cs: EmptyConcurrentHashMap, bh: Blackhole): Unit = {
    bh.consume(cs.cache.get(ss.rand.nextLong % (2 * ss.N)))
  }

  @Benchmark
  @Group("cache")
  def writeCache(ss: SharedState, cs: EmptyCache, bh: Blackhole): Unit = {
    cs.cache.update(ss.rand.nextLong % (2 * ss.N), Array(ss.rand.nextDouble))
  }

  @Benchmark
  @Group("cache")
  def readCache(ss: SharedState, cs: EmptyCache, bh: Blackhole): Unit = {
    bh.consume(cs.cache(ss.rand.nextLong % (2 * ss.N)))
  }

}

object MixedBenchmark {
  @State(Scope.Thread)
  class SharedState {
    @Param(Array())
    var N: Int = _

    val rand = new Random(4)
  }

  @State(Scope.Group)
  class EmptyCache {
    var cache: Cache[Long, Double] = _

    @Setup(Level.Iteration)
    def clear(ss: SharedState): Unit = {
      cache = new Cache(ss.N)
    }
  }

  @State(Scope.Group)
  class EmptyConcurrentHashMap {
    var cache: ConcurrentHashMap[Long, Array[Double]] = _

    @Setup(Level.Iteration)
    def clear(ss: SharedState): Unit = {
      cache = new ConcurrentHashMap(ss.N)
    }
  }
}
