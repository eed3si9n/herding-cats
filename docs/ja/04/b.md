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

```scala mdoc
4 * 1

1 * 9

List(1, 2, 3) ++ Nil

Nil ++ List(0.5, 2.5)
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
scala> import cats._, cats.syntax.all._
import cats._
import cats.syntax.all._

scala> import cats.kernel.laws.discipline.MonoidTests
import cats.kernel.laws.discipline.MonoidTests

scala> import org.scalacheck.Test.Parameters
import org.scalacheck.Test.Parameters

scala> val rs1 = MonoidTests[Int].monoid
val rs1: cats.kernel.laws.discipline.MonoidTests[Int]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@108684fb

scala> rs1.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
+ monoid.intercalateCombineAllOption: OK, passed 100 tests.
+ monoid.intercalateIntercalates: OK, passed 100 tests.
+ monoid.intercalateRepeat1: OK, passed 100 tests.
+ monoid.intercalateRepeat2: OK, passed 100 tests.
+ monoid.is id: OK, passed 100 tests.
+ monoid.left identity: OK, passed 100 tests.
+ monoid.repeat0: OK, passed 100 tests.
+ monoid.repeat1: OK, passed 100 tests.
+ monoid.repeat2: OK, passed 100 tests.
+ monoid.reverseCombineAllOption: OK, passed 100 tests.
+ monoid.reverseRepeat1: OK, passed 100 tests.
+ monoid.reverseRepeat2: OK, passed 100 tests.
+ monoid.reverseReverses: OK, passed 100 tests.
+ monoid.right identity: OK, passed 100 tests.
```

MUnit test で書くとこうなる:

```scala
package example

import cats._
import cats.kernel.laws.discipline.MonoidTests

class IntTest extends munit.DisciplineSuite {
  checkAll("Int", MonoidTests[Int].monoid)
}
```

#### 値クラス

LYAHFGG:

> Haskell の **newtype** キーワードは、まさにこのような「1つの型を取り、それを何かにくるんで別の型に見せかけたい」という場合のために作られたものです。

Cats は tagged type 的な機能を持たないけども、現在の Scala には[値クラス][value-classes-overview-ja]がある。ある一定の条件下ではこれは unboxed
(メモリ割り当てオーバーヘッドが無いこと) を保つので、簡単な例に使う分には問題無いと思う。

```scala
class Wrapper(val unwrap: Int) extends AnyVal
```

#### Disjunction と Conjunction

LYAHFGG:

> モノイドにする方法が2通りあって、どちらも捨てがたいような型は、`Num a` 以外にもあります。`Bool` です。1つ目の方法は `||` をモノイド演算とし、`False` を単位元とする方法です。
> ....
>
> `Bool` を `Monoid` のインスタンスにするもう1つの方法は、`Any` のいわば真逆です。`&&` をモノイド演算とし、`True` を単位元とする方法です。

Cats はこれを提供しないけども、自分で実装してみる。

```scala mdoc
import cats._, cats.syntax.all._

// `class Disjunction(val unwrap: Boolean) extends AnyVal` doesn't work on mdoc
class Disjunction(val unwrap: Boolean)

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

val x1 = Disjunction(true) |+| Disjunction(false)

x1.unwrap

val x2 = Monoid[Disjunction].empty |+| Disjunction(true)

x2.unwrap
```

こっちが Conjunction:

```scala mdoc
// `class Conjunction(val unwrap: Boolean) extends AnyVal` doesn't work on mdoc
class Conjunction(val unwrap: Boolean)

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

val x3 = Conjunction(true) |+| Conjunction(false)

x3.unwrap

val x4 = Monoid[Conjunction].empty |+| Conjunction(true)

x4.unwrap
```

独自 newtype がちゃんと Monoid則を満たしているかチェックするべきだ。

```scala
scala> import cats._, cats.syntax.all._
import cats._
import cats.syntax.all._

scala> import cats.kernel.laws.discipline.MonoidTests
import cats.kernel.laws.discipline.MonoidTests

scala> import org.scalacheck.Test.Parameters
import org.scalacheck.Test.Parameters

scala> import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.{Arbitrary, Gen}

scala> implicit def arbDisjunction(implicit ev: Arbitrary[Boolean]): Arbitrary[Disjunction] =
         Arbitrary { ev.arbitrary map { Disjunction(_) } }
def arbDisjunction(implicit ev: org.scalacheck.Arbitrary[Boolean]): org.scalacheck.Arbitrary[Disjunction]

scala> val rs1 = MonoidTests[Disjunction].monoid
val rs1: cats.kernel.laws.discipline.MonoidTests[Disjunction]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@464d134

scala> rs1.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
....
```

`Disjunction` は動いた。

```scala
scala> implicit def arbConjunction(implicit ev: Arbitrary[Boolean]): Arbitrary[Conjunction] =
         Arbitrary { ev.arbitrary map { Conjunction(_) } }
def arbConjunction(implicit ev: org.scalacheck.Arbitrary[Boolean]): org.scalacheck.Arbitrary[Conjunction]

scala> val rs2 = MonoidTests[Conjunction].monoid
val rs2: cats.kernel.laws.discipline.MonoidTests[Conjunction]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@71a4f643

scala> rs2.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
....
```

`Conjunction` も大丈夫そうだ。

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

```scala mdoc
none[String] |+| "andy".some

1.some |+| none[Int]
```

ちゃんと動く。

LYAHFGG:

> 中身がモノイドがどうか分からない状態では、`mappend` は使えません。どうすればいいでしょう？ 1つの選択は、第一引数を返して第二引数は捨てる、と決めておくことです。この用途のために `First a` というものが存在します。

Haskell は `newtype` を使って `First` 型コンストラクタを実装している。
ジェネリックな値クラスの場合はメモリ割り当てを回避することができないので、普通に case class を使おう。

```scala mdoc
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

First('a'.some) |+| First('b'.some)

First(none[Char]) |+| First('b'.some)
```

Monoid則を検査:

```scala
scala> implicit def arbFirst[A: Eq](implicit ev: Arbitrary[Option[A]]): Arbitrary[First[A]] =
         Arbitrary { ev.arbitrary map { First(_) } }
def arbFirst[A](implicit evidence\$1: cats.Eq[A], ev: org.scalacheck.Arbitrary[Option[A]]): org.scalacheck.Arbitrary[First[A]]

scala> val rs3 = MonoidTests[First[Int]].monoid
val rs3: cats.kernel.laws.discipline.MonoidTests[First[Int]]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@17d3711d

scala> rs3.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
....
```

`First` もシリアライズできないらしい。

LYAHFGG:

> 逆に、2つの `Just` を `mappend` したときに後のほうの引数を優先するような `Maybe a` が欲しい、という人のために、`Data.Monoid` には `Last a` 型も用意されています。

```scala mdoc
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

Last('a'.some) |+| Last('b'.some)

Last('a'.some) |+| Last(none[Char])
```

また、法則検査。

```scala
scala> implicit def arbLast[A: Eq](implicit ev: Arbitrary[Option[A]]): Arbitrary[Last[A]] =
         Arbitrary { ev.arbitrary map { Last(_) } }
def arbLast[A](implicit evidence\$1: cats.Eq[A], ev: org.scalacheck.Arbitrary[Option[A]]): org.scalacheck.Arbitrary[Last[A]]

scala> val rs4 = MonoidTests[Last[Int]].monoid
val rs4: cats.kernel.laws.discipline.MonoidTests[Last[Int]]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@7b28ea53

scala> rs4.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
....
```

モノイドが何なのか感じがつかめて気がする。
