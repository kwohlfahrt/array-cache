package cache

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

class Cache[K <: AnyVal : ClassTag, V <: AnyVal : ClassTag](
    ttlMillis: Long,
    minSize: Int = 1,
    maxSize: Int = Int.MaxValue,
    clock: () => Long = System.currentTimeMillis,
    smoothing: Double = 0.5,
    resizeThreshold: Double = 0.02,
  )(implicit kev: Numeric[K], vev: Numeric[V]){
  var capacity = minSize
  private var fillFactor = 0.0
  private var metas = Array.fill[Long](capacity)(0)
  private var keys = Array.fill[K](capacity)(kev.zero)
  private var values = Array.fill[V](capacity)(vev.zero)

  def apply(key: K): Option[V] = {
    val bucket = key.hashCode.abs % capacity
    if (isExpired(bucket)) {
      None
    } else if (keys(bucket) != key) {
      None
    } else {
      Some(values(bucket))
    }
  }

  def update(key: K, value: V): Unit = {
    val bucket = key.hashCode.abs % capacity
    val isCollision = !isExpired(bucket) && keys(bucket) != key
    fillFactor += smoothing * ((if (isCollision) 1.0 else 0.0) - fillFactor)
    if (fillFactor > resizeThreshold) {
      resize(capacity * 2)
    }
    metas(bucket) = clock()
    keys(bucket) = key
    values(bucket) = value
  }

  def resize(size: Int): Unit = {
    println(size)
    val newMetas = Array.fill[Long](size)(0)
    val newKeys = Array.fill[K](size)(kev.zero)
    val newValues = Array.fill[V](size)(vev.zero)
    for (bucket <- (0 to (capacity - 1)).filter(!isExpired(_))) {
      val newBucket = keys(bucket).hashCode.abs % size
      newMetas(newBucket) = metas(bucket)
      newKeys(newBucket) = keys(bucket)
      newValues(newBucket) = values(bucket)
    }
    metas = newMetas
    keys = newKeys
    values = newValues
    capacity = size
    fillFactor = 0.0
  }

  private def isExpired(bucket: Int): Boolean = {
    metas(bucket) < clock() - ttlMillis
  }
}
