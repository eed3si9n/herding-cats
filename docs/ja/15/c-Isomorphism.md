---
out: Isomorphism.html
---

### 同型射

Lawvere:

> **定義**: ある射 *f: A => B* に対して *g ∘ f = 1<sub>A</sub>* と *f ∘ g = 1<sub>B</sub>* の両方を満たす射 *g: B => A* が存在するとき、f を同型射 (isomorphism) または可逆な射 (invertible arrow) であるという。
> また、1つでも同型射 *f: A => B* が存在するとき、2つの対象 *A* と *B* は**同型** (isomorphic) であるという。

残念ながら Cats には同型射を表すデータ型が無いため、自前で定義する必要がある。

```console:new
scala> import cats._, cats.instances.all._, cats.arrow.Arrow
scala> :paste
object Isomorphisms {
  trait Isomorphism[Arrow[_, _], A, B] { self =>
    def to: Arrow[A, B]
    def from: Arrow[B, A]
  }
  type IsoSet[A, B] = Isomorphism[Function1, A, B]
  type <=>[A, B] = IsoSet[A, B]
}
import Isomorphisms._
```

`Family` から `Relic` への同型射は以下のように定義できる。

```console
scala> :paste
sealed trait Family {}
case object Mother extends Family {}
case object Father extends Family {}
case object Child extends Family {}
sealed trait Relic {}
case object Feather extends Relic {}
case object Stone extends Relic {}
case object Flower extends Relic {}
val isoFamilyRelic = new (Family <=> Relic) {
  val to: Family => Relic = {
    case Mother => Feather
    case Father => Stone
    case Child  => Flower
  }
  val from: Relic => Family = {
    case Feather => Mother
    case Stone   => Father
    case Flower  => Child
  }
}
```

### 射の等価性

これをテストするためには、まず2つの関数を比較するテストを実装することができる。2つの射は 3つの材料が同一である場合に等価であると言える。

- ドメイン *A*
- コドメイン *B*
- *f ∘ a* を割り当てるルール

ScalaCheck だとこう書ける:

```scala
scala> import org.scalacheck.{Prop, Arbitrary, Gen}
import org.scalacheck.{Prop, Arbitrary, Gen}

scala> import cats.syntax.eq._
import cats.syntax.eq._

scala> def func1EqualsProp[A, B](f: A => B, g: A => B)
         (implicit ev1: Eq[B], ev2: Arbitrary[A]): Prop =
         Prop.forAll { a: A =>
           f(a) === g(a)
         }
func1EqualsProp: [A, B](f: A => B, g: A => B)(implicit ev1: cats.Eq[B], implicit ev2: org.scalacheck.Arbitrary[A])org.scalacheck.Prop

scala> val p1 = func1EqualsProp((_: Int) + 2, 1 + (_: Int))
p1: org.scalacheck.Prop = Prop

scala> p1.check
! Falsified after 0 passed tests.
> ARG_0: 0

scala> val p2 = func1EqualsProp((_: Int) + 2, 2 + (_: Int))
p2: org.scalacheck.Prop = Prop

scala> p2.check
+ OK, passed 100 tests.
```

### 同型射のテスト

```scala
scala> :paste
implicit val familyEqual = Eq.fromUniversalEquals[Family]
implicit val relicEqual = Eq.fromUniversalEquals[Relic]
implicit val arbFamily: Arbitrary[Family] = Arbitrary {
  Gen.oneOf(Mother, Father, Child)
}
implicit val arbRelic: Arbitrary[Relic] = Arbitrary {
  Gen.oneOf(Feather, Stone, Flower)
}

// Exiting paste mode, now interpreting.

familyEqual: cats.kernel.Eq[Family] = cats.kernel.Eq\$\$anon\$116@99f2e3d
relicEqual: cats.kernel.Eq[Relic] = cats.kernel.Eq\$\$anon\$116@159bd786
arbFamily: org.scalacheck.Arbitrary[Family] = org.scalacheck.ArbitraryLowPriority\$\$anon\$1@799b3915
arbRelic: org.scalacheck.Arbitrary[Relic] = org.scalacheck.ArbitraryLowPriority\$\$anon\$1@36c230c0

scala> func1EqualsProp(isoFamilyRelic.from compose isoFamilyRelic.to, identity[Family] _).check
+ OK, passed 100 tests.

scala> func1EqualsProp(isoFamilyRelic.to compose isoFamilyRelic.from, identity[Relic] _).check
+ OK, passed 100 tests.
```

テストはうまくいったみたいだ。今日はここまで。
