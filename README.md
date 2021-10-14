# array-cache
An array-based cache to store large numbers of embeddings without GC pressure.

> **WARNING:** this library is a proof-of-concept, and has known breaking bugs.

It is intended for the caching of large numbers of sparse embeddings, in the
format `Seq[(Long, Double)]`, with a fixed maximum element count. Each stored
embedding is identified by a `Long` ID. We use a single large array to avoid
keeping track of excessive numbers of long-lived objects, that don't play well
with generational GCs.

Currently, only an  `Array[Double]` is stored for each key, but the principle
should be straightforward to extend.

## Design

The main use-case for this library is caching. This gives a few notable
constraints:

1. All items expire at some point, therefore it is best to overwrite old values
   first.
2. We don't need to store _all_ items. If a small fraction are dropped, this is
   not a problem, though this fraction should be kept low.
3. We never overwrite items before they expire (as we only check backing
   storage if an item is not in the cache).

The map itself contains an `Array[Long]`, which tracks where each item is
stored in the backing `Ring` (a ring-buffer). The location in the array is
determined by the hash of the incoming key.

The backing ring-buffer simply reserves a slot whenever a new item is pushed,
overwriting the oldest data. On retrieval, we check the stored key against the
stored value.

## Benchmarks

At larger sizes (10M), this cache is competitive with `ConcurrentHashMap`,
especially for mixed or write-heavy workloads. The below results are using 6
parallel threads.

```
[info] Benchmark                             (N)   Mode  Cnt   Score    Error   Units
[info] MixedBenchmark.cache             10000000  thrpt    3  14.277 ± 11.703  ops/us
[info] MixedBenchmark.cache:readCache   10000000  thrpt    3   4.649 ±  4.668  ops/us
[info] MixedBenchmark.cache:writeCache  10000000  thrpt    3   9.628 ± 16.214  ops/us
[info] MixedBenchmark.ref               10000000  thrpt    3   9.139 ±  3.465  ops/us
[info] MixedBenchmark.ref:readRef       10000000  thrpt    3   5.892 ±  2.861  ops/us
[info] MixedBenchmark.ref:writeRef      10000000  thrpt    3   3.247 ±  0.606  ops/us
[info] ReadBenchmark.cache              10000000  thrpt    3  12.696 ±  2.423  ops/us
[info] ReadBenchmark.ref                10000000  thrpt    3  30.871 ±  7.450  ops/us
[info] WriteBenchmark.cache             10000000  thrpt    3  13.744 ±  1.431  ops/us
[info] WriteBenchmark.ref               10000000  thrpt    3   5.774 ± 13.307  ops/us
```

At smaller sizes (1000), `ConcurrentHashMap` is _much_ (~5x) faster. I haven't
looked at where this difference comes from in much detail.
