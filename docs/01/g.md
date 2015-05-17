
  [IntervalsTalk]: https://newcircle.com/s/post/1729/intervals_unifying_uncertainty_ranges_and_loops_erik_osheim_video
  [spire]: https://github.com/non/spire

### Enum

LYAHFGG:

> `Enum` members are sequentially ordered types — they can be enumerated. The main advantage of the `Enum` typeclass is that we can use its types in list ranges. They also have defined successors and predecessors, which you can get with the `succ` and `pred` functions.

I could not find Cats' equivalent for this typeclass.

It's not an `Enum` or range, but [non/spire][spire] has an interesting data structure called `Interval`.
Check out Erik's [Intervals: Unifying Uncertainty, Ranges, and Loops][IntervalsTalk] talk from nescala 2015.
