
### Pos

Awodey は和訳が見つからなかったので勝手訳になる:

> 数学でよく見るものに**構造的集合** (structured set)、つまり集合に何らかの「構造」を追加したものと、それを「保存する」関数の圏というものがある。（構造と保存の定義は独自に与えられる）

> 半順序集合 (partially ordered set)、または略して *poset* と呼ばれる集合 *A* は、全ての *a, b, c ∈ A* に対して以下の条件が成り立つ二項関係 *a ≤<sub>A</sub> b* を持つ:
>
> - 反射律 (reflexivity): a ≤<sub>A</sub> a
> - 推移律 (transitivity): もし a ≤<sub>A</sub> b かつ b ≤<sub>A</sub> c ならば a ≤<sub>A</sub> c
> - 反対称律 (antisymmetry): もし a ≤<sub>A</sub> b かつ b ≤<sub>A</sub> a ならば a = b
>
> poset *A* から poset *B* への射は単調 (monotone) な関数 m: A => B で、これは全ての a, a' ∈ A に対して以下が成り立つという意味だ:
>
> - a ≤<sub>A</sub> a' のとき m(a) ≤<sub>A</sub> m(a')

関数が**単調** (monotone) であるかぎり対象は圏の中にとどまるため、「構造」が保存されると言える。poset と単調関数の圏は **Pos** と表記される。Awodey は poset が好きなので、これを理解しておくのは重要。

poset の例としては `Int` 型があり、`≤` として `PartialOrder` 型クラスで定義されているように整数の比較である `<=` を使う。
別の例として、`case class LString(value: String)` を考えてみる。`≤` としては `value` の文字列の長さを比較に使う。

```scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

case class LString(value: String)
val f: Int => LString = (x: Int) => LString(if (x < 0) "" else x.toString)

// Exiting paste mode, now interpreting.

defined class LString
f: Int => LString = <function1>

scala> f(0)
res0: LString = LString(0)

scala> f(10)
res1: LString = LString(10)
```

上の `f` は、`f(0) ≤ f(10)` および `a <= a'` を満たす任意の `Int` において `f(a) ≤ f(a')` であるため、単調である。
