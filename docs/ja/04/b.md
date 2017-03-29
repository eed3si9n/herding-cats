---
out: Monoid.html
---

  [value-classes-overview-ja]: http://docs.scala-lang.org/ja/overviews/core/value-classes.html
  [algebra13]: https://github.com/non/algebra/issues/13

### Monoid

LYAHFGG:

> どうやら、`*` に `1` という組み合わせと、`++` に `[]` という組み合わせは、共通の性質を持っているようですね。
>
> - 関数は引数を2つ取る。
> - 2つの引数および返り値の型はすべて等しい。
> - 2引数関数を施して相手を変えないような特殊な値が存在する。

これを Scala で確かめてみる:

```console:new
scala> 4 * 1
scala> 1 * 9
scala> List(1, 2, 3) ++ Nil
scala> Nil ++ List(0.5, 2.5)
```

あってるみたいだ。

#### Monoid 型クラス

以下が `algebera.Monoid` の型クラス・コントラクトだ:

```scala
/**
 * A monoid is a semigroup with an identity. A monoid is a specialization of a
 * semigroup, so its operation must be associative. Additionally,
 * `combine(x, empty) == combine(empty, x) == x`. For example, if we have `Monoid[String]`,
 * with `combine` as string concatenation, then `empty = ""`.
 */
trait Monoid[@sp(Int, Long, Float, Double) A] extends Any with Semigroup[A] {

  /**
   * Return the identity element for this monoid.
   */
  def empty: A

  ...
}
```

#### Monoid則

Semigroup則に加えて、Monoid則はもう 2つの法則がある:

- associativity `(x |+| y) |+| z = x |+| (y |+| z)`
- left identity `Monoid[A].empty |+| x = x`
- right identity `x |+| Monoid[A].empty = x`

REPL から Monoid則を検査してみよう:

```scala
scala> import cats._, cats.data._, cats.implicits._
import cats._
import cats.data._
import cats.implicits._

scala> import cats.kernel.laws.GroupLaws
import cats.kernel.laws.GroupLaws

scala> val rs1 = GroupLaws[Int].monoid(Monoid[Int])
rs1: cats.kernel.laws.GroupLaws[Int]#GroupProperties = cats.kernel.laws.GroupLaws\$GroupProperties@17a695f0

scala> rs1.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
+ monoid.serializable: OK, proved property.
```

Spec2 specification で書くとこうなる:

```scala
package example

import cats._
import algebra.laws.GroupLaws

class IntSpec extends CatsSpec { def is = s2"""
  (Int, +) should
     form a monoid                                         \$e1
  """

  def e1 = checkAll("Int", GroupLaws[Int].monoid(Monoid[Int]))
}
```

#### 値クラス

LYAHFGG:

> Haskell の **newtype** キーワードは、まさにこのような「1つの型を取り、それを何かにくるんで別の型に見せかけたい」という場合のために作られたものです。

Cats は tagged type 的な機能を持たないけども、現在の Scala には[値クラス][value-classes-overview-ja]がある。ある一定の条件下ではこれは unboxed
(メモリ割り当てオーバーヘッドが無いこと) を保つので、簡単な例に使う分には問題無いと思う。

```console:new
scala> :paste
class Wrapper(val unwrap: Int) extends AnyVal
```

#### Disjunction と Conjunction

LYAHFGG:

> モノイドにする方法が2通りあって、どちらも捨てがたいような型は、`Num a` 以外にもあります。`Bool` です。1つ目の方法は `||` をモノイド演算とし、`False` を単位元とする方法です。
> ....
>
> `Bool` を `Monoid` のインスタンスにするもう1つの方法は、`Any` のいわば真逆です。`&&` をモノイド演算とし、`True` を単位元とする方法です。

Cats はこれを提供しないけども、自分で実装してみる。

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> :paste
class Disjunction(val unwrap: Boolean) extends AnyVal
object Disjunction {
  @inline def apply(b: Boolean): Disjunction = new Disjunction(b)
  implicit val disjunctionMonoid: Monoid[Disjunction] = new Monoid[Disjunction] {
    def combine(a1: Disjunction, a2: Disjunction): Disjunction =
      Disjunction(a1.unwrap || a2.unwrap)
    def empty: Disjunction = Disjunction(false)
  }
  implicit val disjunctionEq: Eq[Disjunction] = new Eq[Disjunction] {
    def eqv(a1: Disjunction, a2: Disjunction): Boolean =
      a1.unwrap == a2.unwrap
  }
}
scala> val x1 = Disjunction(true) |+| Disjunction(false)
scala> x1.unwrap
scala> val x2 = Monoid[Disjunction].empty |+| Disjunction(true)
scala> x2.unwrap
```

こっちが Conjunction:

```console
scala> :paste
class Conjunction(val unwrap: Boolean) extends AnyVal
object Conjunction {
  @inline def apply(b: Boolean): Conjunction = new Conjunction(b)
  implicit val conjunctionMonoid: Monoid[Conjunction] = new Monoid[Conjunction] {
    def combine(a1: Conjunction, a2: Conjunction): Conjunction =
      Conjunction(a1.unwrap && a2.unwrap)
    def empty: Conjunction = Conjunction(true)
  }
  implicit val conjunctionEq: Eq[Conjunction] = new Eq[Conjunction] {
    def eqv(a1: Conjunction, a2: Conjunction): Boolean =
      a1.unwrap == a2.unwrap
  }
}
scala> val x3 = Conjunction(true) |+| Conjunction(false)
scala> x3.unwrap
scala> val x4 = Monoid[Conjunction].empty |+| Conjunction(true)
scala> x4.unwrap
```

独自 newtype がちゃんと Monoid則を満たしているかチェックするべきだ。

```scala
scala> import algebra.laws.GroupLaws
import algebra.laws.GroupLaws

scala> import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.{Arbitrary, Gen}

scala> implicit def arbDisjunction(implicit ev: Arbitrary[Boolean]): Arbitrary[Disjunction] =
         Arbitrary { ev.arbitrary map { Disjunction(_) } }
arbDisjunction: (implicit ev: org.scalacheck.Arbitrary[Boolean])org.scalacheck.Arbitrary[Disjunction]

scala> val rs1 = GroupLaws[Disjunction].monoid
rs1: algebra.laws.GroupLaws[Disjunction]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@77663edf

scala> rs1.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
! monoid.serializable: Falsified after 0 passed tests.
```

モノイドが `Serializable` では無いという理由でテストが失敗した。
Monoid則が何故 `Serializable` の検査をしているのかが分からず、混乱している。
[non/algebra#13][algebra13] によると Spark に便利と書いてあるけど、別に分けたほうがいいのではないか。

> **追記**: REPL を使って型クラスのインスタンスを定義してることが失敗の原因であると判明した!

```scala
scala> implicit def arbConjunction(implicit ev: Arbitrary[Boolean]): Arbitrary[Conjunction] =
         Arbitrary { ev.arbitrary map { Conjunction(_) } }
arbConjunction: (implicit ev: org.scalacheck.Arbitrary[Boolean])org.scalacheck.Arbitrary[Conjunction]

scala> val rs2 = GroupLaws[Conjunction].monoid
rs2: algebra.laws.GroupLaws[Conjunction]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@15f279d

scala> rs2.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
! monoid.serializable: Falsified after 0 passed tests.
```

Serializable ルールを抜けば、`Conjunction` も大丈夫そうだ。

#### Monoid としての Option

LYAHFGG:

> `Maybe a` をモノイドにする1つ目の方法は、型引数 `a` がモノイドであるときに限り `Maybe a` もモノイドであるとし、`Maybe a` の `mappend` を、`Just` の中身の `mappend` を使って定義することです。

Cats がこうなっているか確認しよう。

```scala
  implicit def optionMonoid[A](implicit ev: Semigroup[A]): Monoid[Option[A]] =
    new Monoid[Option[A]] {
      def empty: Option[A] = None
      def combine(x: Option[A], y: Option[A]): Option[A] =
        x match {
          case None => y
          case Some(xx) => y match {
            case None => x
            case Some(yy) => Some(ev.combine(xx,yy))
          }
        }
    }
```

`mappend` を `combine` と読み替えれば、あとはパターンマッチだけだ。
使ってみよう。

```console
scala> none[String] |+| "andy".some
scala> 1.some |+| none[Int]
```

ちゃんと動く。

LYAHFGG:

> 中身がモノイドがどうか分からない状態では、`mappend` は使えません。どうすればいいでしょう？ 1つの選択は、第一引数を返して第二引数は捨てる、と決めておくことです。この用途のために `First a` というものが存在します。

Haskell は `newtype` を使って `First` 型コンストラクタを実装している。
ジェネリックな値クラスの場合はメモリ割り当てを回避することができないので、普通に case class を使おう。

```console
scala> :paste
case class First[A: Eq](val unwrap: Option[A])
object First {
  implicit def firstMonoid[A: Eq]: Monoid[First[A]] = new Monoid[First[A]] {
    def combine(a1: First[A], a2: First[A]): First[A] =
      First((a1.unwrap, a2.unwrap) match {
        case (Some(x), _) => Some(x)
        case (None, y)    => y
      })
    def empty: First[A] = First(None: Option[A])
  }
  implicit def firstEq[A: Eq]: Eq[First[A]] = new Eq[First[A]] {
    def eqv(a1: First[A], a2: First[A]): Boolean =
      Eq[Option[A]].eqv(a1.unwrap, a2.unwrap)
  }
}
scala> First('a'.some) |+| First('b'.some)
scala> First(none[Char]) |+| First('b'.some)
```

Monoid則を検査:

```scala
scala> implicit def arbFirst[A: Eq](implicit ev: Arbitrary[Option[A]]): Arbitrary[First[A]] =
         Arbitrary { ev.arbitrary map { First(_) } }
arbFirst: [A](implicit evidence\$1: cats.Eq[A], implicit ev: org.scalacheck.Arbitrary[Option[A]])org.scalacheck.Arbitrary[First[A]]

scala> val rs3 = GroupLaws[First[Int]].monoid
rs3: algebra.laws.GroupLaws[First[Int]]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@44736530

scala> rs3.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
! monoid.serializable: Falsified after 0 passed tests.
```

`First` もシリアライズできないらしい。

LYAHFGG:

> 逆に、2つの `Just` を `mappend` したときに後のほうの引数を優先するような `Maybe a` が欲しい、という人のために、`Data.Monoid` には `Last a` 型も用意されています。

```console
scala> :paste
case class Last[A: Eq](val unwrap: Option[A])
object Last {
  implicit def lastMonoid[A: Eq]: Monoid[Last[A]] = new Monoid[Last[A]] {
    def combine(a1: Last[A], a2: Last[A]): Last[A] =
      Last((a1.unwrap, a2.unwrap) match {
        case (_, Some(y)) => Some(y)
        case (x, None)    => x
      })
    def empty: Last[A] = Last(None: Option[A])
  }
  implicit def lastEq[A: Eq]: Eq[Last[A]] = new Eq[Last[A]] {
    def eqv(a1: Last[A], a2: Last[A]): Boolean =
      Eq[Option[A]].eqv(a1.unwrap, a2.unwrap)
  }
}
scala> Last('a'.some) |+| Last('b'.some)
scala> Last('a'.some) |+| Last(none[Char])
```

また、法則検査。

```scala
scala> implicit def arbLast[A: Eq](implicit ev: Arbitrary[Option[A]]): Arbitrary[Last[A]] =
         Arbitrary { ev.arbitrary map { Last(_) } }
arbLast: [A](implicit evidence\$1: cats.Eq[A], implicit ev: org.scalacheck.Arbitrary[Option[A]])org.scalacheck.Arbitrary[Last[A]]

scala> val rs4 = GroupLaws[Last[Int]].monoid
rs4: algebra.laws.GroupLaws[Last[Int]]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@121fd6d9

scala> rs4.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
! monoid.serializable: Falsified after 0 passed tests.
```

モノイドが何なのか感じがつかめて気がする。
