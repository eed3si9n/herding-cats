
### Grp

Awodey:

> **定義 1.4** **群** (group) G は、モノイドのうち全ての要素 g に対して逆射 (inverse) g<sup>-1</sup> を持つもの。つまり、G は唯一つの対象を持つ圏で、全ての射が同型射となっている。

`cats.kernel.Monoid` の型クラスコントラクトはこうなっている:

```scala
/**
 * A group is a monoid where each element has an inverse.
 */
trait Group[@sp(Int, Long, Float, Double) A] extends Any with Monoid[A] {

  /**
   * Find the inverse of `a`.
   *
   * `combine(a, inverse(a))` = `combine(inverse(a), a)` = `empty`.
   */
  def inverse(a: A): A
}
```

syntax がインポートされていてば、これは `inverse` メソッドを可能とする:

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> 1.inverse
scala> assert((1 |+| 1.inverse) === Monoid[Int].empty)
```

群 (group) と群の準同型写像 (group homomorphism、群の構造を保存する関数) の圏は **Grp** と表記される。

### 忘却函手

準同型写像という用語が何回か出てきたが、構造を保存しない関数を考えることもできる。
全ての群 *G* はモノイドでもあるので、*f: G => M* という *G* から逆射の能力を失わせて中のモノイドだけを返す関数を考えることができる。さらに、群とモノイドは両方とも圏であるので、*f* は函手であると言える。

これを **Grp** 全体に広げて、*F: Grp => Mon* という函手を考えることができる。このような構造を失わせるような函手を**忘却函手** (forgetful functor) という。Scala でこれを考えると、`A: Group` から始めて、何らかの方法で戻り値を `A: Monoid` にダウングレードさせる感じだろうか。

今日はここまで。
