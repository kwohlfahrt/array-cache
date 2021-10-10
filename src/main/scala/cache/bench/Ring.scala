package cache.bench

import cache._
import scala.util.Random
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit


class RingBenchmark {
  import RingBenchmark._

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def fill(ss: SharedState, cs: RingState, bh: Blackhole): Unit = {
    cs.ring.push((ss.rand.nextLong, ss.rand.nextDouble))
  }

  @Benchmark
  @Threads(2)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def read(ss: SharedState, cs: RingState, bh: Blackhole): Unit = {
    bh.consume(cs.fullRing(cs.headIndex - ss.rand.nextLong % 2000))
  }
}

object RingBenchmark {
  @State(Scope.Thread)
  class SharedState {
    val rand = new Random(4)
  }

  @State(Scope.Thread)
  class RingState {
    val ring = new Ring[Long, Double](1000)
    val fullRing = new Ring[Long, Double](1000)
    var headIndex = 0L

    @Setup(Level.Iteration)
    def clear(): Unit = ring.clear()

    @Setup(Level.Trial)
    def fill(ss: SharedState): Unit = {
      for (_ <- 0 to 1000) {
        headIndex = fullRing.push((ss.rand.nextLong % 2000, ss.rand.nextDouble))
      }
    }
  }
}
