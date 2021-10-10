package cache

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import java.lang.invoke.MethodHandles

/* Cache class is the interface for storing K/V pairs, with expiry times.
 *
 * It uses an inner ring-buffer (Ring), to take advantage of the fact that
 * overwriting old values is OK. Pointers are stored as offsets into the
 * ring-buffer, so entries can be set atomically regardless of data size.
 */
class Cache[K <: AnyVal : ClassTag, V <: AnyVal : ClassTag](
  capacity: Int = 1,
  nNeighbours: Int = 4,
)(implicit kev: Numeric[K], vev: Numeric[V]){
  import Cache._

  private val ring = new Ring[K, V](capacity)
  /* The offset can be at most a Long, to allow atomic instructions. The top
   * bit of the offset contains an occupancy bit.
   */
  private val offsets = Array.fill[Long](capacity)(0)
  private val occupiedMask = ~(~0L >>> 1)
  private val indexMask = ~occupiedMask

  def apply(key: K): Option[V] = {
    val bucket = hash(key)
    for (i <- bucket until (bucket + nNeighbours)) {
      /* This requires a load-load barrier, to ensure the read of the data
       * occurs after the load of a valid offset.
       *
       * Consume semantics would be sufficient here.
       */
      val offset: Long = offsetHandle.getAcquire(offsets, i % capacity)
      ring(offset) match {
        case Some((rkey, value)) if (key == rkey) => return Some(value)
        case _ => ()
      }
    }
    None
  }

  def update(key: K, value: V): Unit = {
    val newOffset = ring.push(key, value)
    val bucket = hash(key)
    for (i <- bucket until (bucket + nNeighbours)) {
      val offset: Long = offsetHandle.getOpaque(offsets, i % capacity)
      ring(offset) match {
        /* We never overwrite non-expired keys, so don't have to consider
         * re-using a slot if the key is the same.
         */
        case Some(_) => ()
        /* This requires a store-store barrier, to ensure the pushed value is
         * visible if this offset is read.
         */
        case None => return offsetHandle.setRelease(offsets, i % capacity, newOffset)
        /* We don't need to store every key, only most keys. If we do not find
         * a free slot within nNeighbours, we give up. TODO - we could also
         * replace the oldest.
         */
      }
    }
  }

  def hash(key: K): Int = (key.hashCode.intValue - 1).abs % capacity

  def clear(): Unit = {
    for (i <- 0 until offsets.length) offsets(i) = 0
    ring.clear()
  }
}

object Cache {
  val offsetHandle = MethodHandles.arrayElementVarHandle(classOf[Array[Long]])
}
