package cache.bench

import cache._
import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit


class RingBenchmark {
  import RingBenchmark._

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def fill(ss: SharedState, cs: EmptyRing, bh: Blackhole): Unit = {
    cs.ring.push((ss.rand.nextLong, ss.rand.nextDouble))
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def read(ss: SharedState, cs: FullRing, bh: Blackhole): Unit = {
    bh.consume(cs.ring(cs.headIndex - ss.rand.nextLong % (2 * N)))
  }
}

object RingBenchmark {
  var N: Int = 1000

  @State(Scope.Thread)
  class SharedState {
    val rand = new Random(4)
  }

  @State(Scope.Benchmark)
  class EmptyRing {
    val ring = new Ring[Long, Double](N)
    var headIndex = 0L

    @Setup(Level.Iteration)
    def clear(): Unit = ring.clear()
  }

  @State(Scope.Benchmark)
  class FullRing {
    val ring = new Ring[Long, Double](N)
    var headIndex = 0L

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      for (_ <- 0 to N) {
        headIndex = ring.push((ss.rand.nextLong % (2 * N), ss.rand.nextDouble))
      }
    }
  }
}
