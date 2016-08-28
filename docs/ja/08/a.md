---
out: Ior.html
---

  [IorSource]: $catsBaseUrl$core/src/main/scala/cats/data/Ior.scala

### Ior データ型

Cats には `A` と `B` のペアを表すデータ型がもう1つあって、
[Ior][IorSource] と呼ばれている。

```scala
/** Represents a right-biased disjunction that is either an `A`, or a `B`, or both an `A` and a `B`.
 *
 * An instance of `A [[Ior]] B` is one of:
 *  - `[[Ior.Left Left]][A]`
 *  - `[[Ior.Right Right]][B]`
 *  - `[[Ior.Both Both]][A, B]`
 *
 * `A [[Ior]] B` is similar to `A [[Xor]] B`, except that it can represent the simultaneous presence of
 * an `A` and a `B`. It is right-biased like [[Xor]], so methods such as `map` and `flatMap` operate on the
 * `B` value. Some methods, like `flatMap`, handle the presence of two [[Ior.Both Both]] values using a
 * `[[Semigroup]][A]`, while other methods, like [[toXor]], ignore the `A` value in a [[Ior.Both Both]].
 *
 * `A [[Ior]] B` is isomorphic to `(A [[Xor]] B) [[Xor]] (A, B)`, but provides methods biased toward `B`
 * values, regardless of whether the `B` values appear in a [[Ior.Right Right]] or a [[Ior.Both Both]].
 * The isomorphic [[Xor]] form can be accessed via the [[unwrap]] method.
 */
sealed abstract class Ior[+A, +B] extends Product with Serializable {

  final def fold[C](fa: A => C, fb: B => C, fab: (A, B) => C): C = this match {
    case Ior.Left(a) => fa(a)
    case Ior.Right(b) => fb(b)
    case Ior.Both(a, b) => fab(a, b)
  }

  final def isLeft: Boolean = fold(_ => true, _ => false, (_, _) => false)
  final def isRight: Boolean = fold(_ => false, _ => true, (_, _) => false)
  final def isBoth: Boolean = fold(_ => false, _ => false, (_, _) => true)

  ....
}

object Ior extends IorInstances with IorFunctions {
  final case class Left[+A](a: A) extends (A Ior Nothing)
  final case class Right[+B](b: B) extends (Nothing Ior B)
  final case class Both[+A, +B](a: A, b: B) extends (A Ior B)
}
```

これらの値は `Ior` の `left`、`right`、`both` メソッドを使って定義する:

```console:new
scala> import cats._, cats.data.{ Ior, NonEmptyList => NEL }, cats.instances.all._
scala> Ior.right[NEL[String], Int](1)
scala> Ior.left[NEL[String], Int](NEL("error"))
scala> Ior.both[NEL[String], Int](NEL("warning"), 1)
```

scaladoc コメントに書いてある通り、`Ior` の `flatMap` は
`Ior.both(...)` 値をみると `Semigroup[A]` を用いて失敗値を累積 (accumulate) する。
そのため、これは `Xor` と `Validated` のハイブリッドのような感覚で使えるかもしれない。

忘れてて何回かハマってるのは、デフォルトでは `NonEmptyList` 
の `Semigroup` のインスタンスが定義されていないことだ。
自分で `SemigroupK` から導き出さなかればいけない。

`flatMap` の振る舞いを 9つ全ての組み合わせでみてみよう:

```console
scala> import cats.syntax.flatMap._
scala> implicit val nelSemigroup: Semigroup[NEL[String]] = SemigroupK[NEL].algebra[String]
scala> Ior.right[NEL[String], Int](1) >>=
         { x => Ior.right[NEL[String], Int](x + 1) }
scala> Ior.left[NEL[String], Int](NEL("error 1")) >>=
         { x => Ior.right[NEL[String], Int](x + 1) }
scala> Ior.both[NEL[String], Int](NEL("warning 1"), 1) >>=
         { x => Ior.right[NEL[String], Int](x + 1) }
scala> Ior.right[NEL[String], Int](1) >>=
         { x => Ior.left[NEL[String], Int](NEL("error 2")) }
scala> Ior.left[NEL[String], Int](NEL("error 1")) >>=
         { x => Ior.left[NEL[String], Int](NEL("error 2")) }
scala> Ior.both[NEL[String], Int](NEL("warning 1"), 1) >>=
         { x => Ior.left[NEL[String], Int](NEL("error 2")) }
scala> Ior.right[NEL[String], Int](1) >>=
         { x => Ior.both[NEL[String], Int](NEL("warning 2"), x + 1) }
scala> Ior.left[NEL[String], Int](NEL("error 1")) >>=
         { x => Ior.both[NEL[String], Int](NEL("warning 2"), x + 1) }
scala> Ior.both[NEL[String], Int](NEL("warning 1"), 1) >>=
         { x => Ior.both[NEL[String], Int](NEL("warning 2"), x + 1) }
```

`for` 内包表記からも使える:

```console
scala> import cats.syntax.semigroup._
scala> for {
         e1 <- Ior.right[NEL[String], Int](1)
         e2 <- Ior.both[NEL[String], Int](NEL("event 2 warning"), e1 + 1)
         e3 <- Ior.both[NEL[String], Int](NEL("event 3 warning"), e2 + 1)
       } yield (e1 |+| e2 |+| e3)
```

`Ior.left` は `Xor[A, B]` や `Either[A, B]` の失敗値のようにショート回路になるが、
`Ior.both` は `Validated[A, B]` のように失敗値を累積させる。
