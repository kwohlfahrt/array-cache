package cache.bench

import cache._
import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}


@OutputTimeUnit(TimeUnit.MICROSECONDS)
class ReadBenchmark {
  import ReadBenchmark._

  @Benchmark
  def ref(ss: SharedState, cs: FullConcurrentHashMap, bh: Blackhole): Unit = {
    bh.consume(cs.cache.get(ss.rand.nextLong % (2 * ss.N)))
  }

  @Benchmark
  def cache(ss: SharedState, cs: FullCache, bh: Blackhole): Unit = {
    bh.consume(cs.cache(ss.rand.nextLong % (2 * ss.N)))
  }
}

object ReadBenchmark {
  @State(Scope.Thread)
  class SharedState {
    @Param(Array())
    var N: Int = _

    val rand = new Random(4)
  }

  @State(Scope.Benchmark)
  class FullCache {
    var cache: Cache[Long, Double] = _

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      cache = new Cache(ss.N)
      for (_ <- 0 to ss.N) {
        cache.update(ss.rand.nextLong % (2 * ss.N), Array(ss.rand.nextDouble))
      }
    }
  }


  @State(Scope.Benchmark)
  class FullConcurrentHashMap {
    var cache: ConcurrentHashMap[Long, Array[Double]] = _

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      cache = new ConcurrentHashMap(ss.N)
      for (_ <- 0 to ss.N) {
        cache.put(ss.rand.nextLong % (2 * ss.N), Array(ss.rand.nextDouble))
      }
    }
  }
}
