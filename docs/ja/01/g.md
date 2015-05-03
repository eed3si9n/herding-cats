
  [IntervalsTalk]: https://newcircle.com/s/post/1729/intervals_unifying_uncertainty_ranges_and_loops_erik_osheim_video
  [spire]: https://github.com/non/spire

### Enum

LYAHFGG:

> `Enum` のインスタンスは、順番に並んだ型、つまり要素の値を列挙できる型です。`Enum` 型クラスの主な利点は、その値をレンジの中で使えることです。また、`Enum` のインスタンスの型には後者関数 `succ` と前者関数 `pred` も定義されます。

これは対応する Cats での型クラスを見つけることができなかった。

これは、`Enum` でも範囲でもないが、[non/spire][spire] には `Interval` と呼ばれる面白いデータ構造がある。
nescala 2015 での Erik のトーク、[Intervals: Unifying Uncertainty, Ranges, and Loops][IntervalsTalk] を見てほしい。
