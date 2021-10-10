package cache.bench

import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}


class ReferenceBenchmark {
  import ReferenceBenchmark._

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def fill(ss: SharedState, cs: HashMapState, bh: Blackhole): Unit = {
    cs.cache.put(ss.rand.nextLong, ss.rand.nextDouble)
  }

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def read(ss: SharedState, cs: HashMapState, bh: Blackhole): Unit = {
    bh.consume(cs.fullCache.get(ss.rand.nextLong % 2000))
  }
}

object ReferenceBenchmark {
  @State(Scope.Thread)
  class SharedState {
    val rand = new Random(4)
  }

  @State(Scope.Thread)
  class HashMapState {
    val cache = new ConcurrentHashMap[Long, Double](1000)
    val fullCache = new ConcurrentHashMap[Long, Double](1000)

    @Setup(Level.Iteration)
    def clear(): Unit = cache.clear()

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      for (_ <- 0 to 1000) {
        fullCache.put(ss.rand.nextLong % 2000, ss.rand.nextDouble)
      }
    }
  }
}
