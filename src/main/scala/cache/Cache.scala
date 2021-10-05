package cache

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import java.util.concurrent.atomic.AtomicLongArray

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
  private val ring = new Ring[K, V](capacity)
  /* The offset can be at most a Long, to allow atomic instructions. The top
   * bit of the offset contains an occupancy bit.
   */
  private val offsets = new AtomicLongArray(capacity)
  private val occupiedMask = ~(~0L >>> 1)
  private val indexMask = ~occupiedMask

  def apply(key: K): Option[V] = {
    /* To look up a key:
     * 1. Hash to a bucket.
     * 2. Probe nNeighbours slots for the presence of the key.
     * 3. Read the value.
     * 4. Check the generation, to ensure the value has not been overwritten.
     *    a. we assume the generation number wraps around infrequently, such
     *       that it will not loop around to the same value during one read.
     */
    val bucket = key.hashCode.abs % capacity
    for (i <- bucket until (bucket + nNeighbours)) {
      val offset = offsets.getOpaque(i % capacity)
      ring(offset) match {
        case Some((rkey, value)) => if (key == rkey) { return Some(value) }
        case None => ()
      }
    }
    None
  }

  def update(key: K, value: V): Unit = {
    /* To insert a key, we have two steps:
     * 1. Push the value into the backing Ring, recording the offset
     *   a. We never overwrite non-expired keys, so don't have to optimize for
     *      re-using slots in the ring.
     * 2. Hash the key to a bucket
     * 3. Probe nNeighbours slots for a free slot
     *    a. We don't need to store every key, only most keys. If we do not
     *       find a free slot within nNeighbours, we give up. TODO - we could
     *       also overwrite the oldest.
     * 4. Store the offset
     */
    val newOffset = ring.push(key, value)
    val bucket = key.hashCode.abs % capacity
    for (i <- bucket until (bucket + nNeighbours)) {
      val offset = offsets.getOpaque(i % capacity)
      ring(offset) match {
        case Some(_) => ()
        case None => return offsets.setRelease(i % capacity, newOffset)
      }
    }
  }
}
