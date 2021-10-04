package cache

import scala.util.Random

// A stress-test, to run and observe GC logs
object Stress extends App {
  val rand = new Random()
  val cache = new Cache[Long, Double](1000)
  val pairs = (0 to 1000).map(_ => (rand.nextLong, rand.nextDouble))
  for ((k, v) <- pairs) {
    cache.update(k, v)
  }
  var hits = 0
  for ((k, v) <- pairs) {
    if (cache(k) == Some(v)) {
      hits += 1
    }
  }
  println(hits, pairs.size - hits)
}
