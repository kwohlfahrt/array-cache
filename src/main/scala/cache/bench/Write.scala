package cache.bench

import cache._
import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}


@OutputTimeUnit(TimeUnit.MICROSECONDS)
class WriteBenchmark {
  import WriteBenchmark._

  @Benchmark
  def ref(ss: SharedState, cs: EmptyConcurrentHashMap): Unit = {
    cs.cache.put(ss.rand.nextLong % (2 * ss.N), ss.rand.nextDouble)
  }

  @Benchmark
  def cache(ss: SharedState, cs: EmptyCache, bh: Blackhole): Unit = {
    cs.cache.update(ss.rand.nextLong % (2 * ss.N), ss.rand.nextDouble)
  }
}

object WriteBenchmark {
  @State(Scope.Thread)
  class SharedState {
    @Param(Array())
    var N: Int = _

    val rand = new Random(4)
  }

  @State(Scope.Benchmark)
  class EmptyCache {
    var cache: Cache[Long, Double] = _

    @Setup(Level.Iteration)
    def clear(ss: SharedState): Unit = {
      cache = new Cache(ss.N)
    }
  }

  @State(Scope.Benchmark)
  class EmptyConcurrentHashMap {
    var cache: ConcurrentHashMap[Long, Double] = _

    @Setup(Level.Iteration)
    def clear(ss: SharedState): Unit = {
      cache = new ConcurrentHashMap(ss.N)
    }
  }
}
