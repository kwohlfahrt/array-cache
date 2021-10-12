package cache

import scala.reflect.ClassTag
import java.lang.invoke.{MethodHandles, VarHandle}

/* Ring class is used to back the Cache interface.
 *
 * The aim of this ring-buffer is to take advantage of the fact that
 * overwriting old values is OK. We store specifically K/V pairs, but could
 * store any data.
 */
class Ring[K <: AnyVal : ClassTag, V <: AnyVal : ClassTag](
  minCapacity: Int = 1,
)(implicit kev: Numeric[K], vev: Numeric[V]) {
  import Ring._

  // Round up to next power of two, so wraparound arithmetic works out
  private val log2Capacity = 32 - Integer.numberOfLeadingZeros(Integer.max(minCapacity - 1, 1))
  private val capacity = 1 << log2Capacity
  private val capacityMask = -1L >>> (64 - log2Capacity)

  private var head: Long = capacity.longValue + 1 // Start so that 0 (offset fill-value offset) is invalid
  private val keys = Array.fill(capacity)(kev.zero)
  private val values = Array.fill(capacity)(vev.zero)

  def apply(i: Long, k: K): Option[V] = {
    // Optimization to avoid work when checking if slots are free
    if (isDead(i)) { return None }
    val index = (i & capacityMask).intValue
    if (keys(index) == k) {
      val value = values(index)

      /* We only need to check if the value has been overwritten during reading.
       * We will never try to read the value before setting it.
       */
      VarHandle.loadLoadFence()
      if (isDead(i)) None else Some(value)
    } else {
      None
    }
  }

  def push(item: (K, V)): Long = {
    /* Here, we reserve a slot.
     *
     * This requires a store-store fence. getAndAdd has volatile semantics, so
     * should be sufficient.
     */
    val offset: Long = headHandle.getAndAdd(this, 1)
    val index = (offset & capacityMask).intValue
    val (key, value) = item
    keys(index) = key
    values(index) = value
    offset
  }

  def isDead(i: Long): Boolean = {
    val head: Long = headHandle.getOpaque(this)
    (i < head - capacity) || (i > head)
    // FIXME: Handle the 2^64 wraparound
  }

  def clear(): Unit = {
    head = capacity.longValue + 1
    for (i <- 0 until keys.length) keys(i) = kev.zero
    for (i <- 0 until values.length) values(i) = vev.zero
  }
}

object Ring {
  val headHandle = MethodHandles
    .privateLookupIn(classOf[Ring[_, _]], MethodHandles.lookup())
    .findVarHandle(classOf[Ring[_, _]], "head", classOf[Long])
}
