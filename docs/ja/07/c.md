---
out: Validated.html
---

  [ValidatedSource]: $catsBaseUrl$core/src/main/scala/cats/data/Validated.scala

### Validated データ型

[Validated][ValidatedSource] という、`Either` の代わりに使えるデータ型がもう1つ Cats に定義されている:

```scala
sealed abstract class Validated[+E, +A] extends Product with Serializable {

  def fold[B](fe: E => B, fa: A => B): B =
    this match {
      case Invalid(e) => fe(e)
      case Valid(a) => fa(a)
    }

  def isValid: Boolean = fold(_ => false, _ => true)
  def isInvalid: Boolean = fold(_ => true, _ => false)

  ....
}

object Validated extends ValidatedInstances with ValidatedFunctions{
  final case class Valid[+A](a: A) extends Validated[Nothing, A]
  final case class Invalid[+E](e: E) extends Validated[E, Nothing]
}
```

値はこのように作る:

```console:new
scala> import cats._, cats.data.Validated, cats.std.all._
scala> import Validated.{ valid, invalid }
scala> valid[String, String]("event 1 ok")
scala> invalid[String, String]("event 1 failed!")
```

`Validated` の違いはこれはモナドではなく、applicative functor を形成することだ。
最初のイベントの結果を次へと連鎖するのでは無く、`Validated` は全イベントを検証する:

```console
scala> import cats.syntax.apply._
scala> val result = (valid[String, String]("event 1 ok") |@|
        invalid[String, String]("event 2 failed!") |@|
        invalid[String, String]("event 3 failed!")) map {_ + _ + _}
```

最終結果は `Invalid(event 3 failed!event 2 failed!)` となった。
計算途中でショートさせた `Xor` のモナドと違って、`Validated` は計算を続行して全ての失敗を報告する。
これはおそらくオンラインのベーコンショップでユーザのインプットを検証するのに役立つと思う。

だけど、問題はエラーメッセージが 1つの文字列にゴチャっと一塊になってしまっていることだ。リストでも使うべきじゃないか?

#### NonEmptyList を用いた失敗値の蓄積

ここで使われるのが `NonEmptyList` データ型だ。
今のところは、必ず 1つ以上の要素が入っていることを保証するリストだと考えておけばいいと思う。

```console
scala> import cats.data.{ NonEmptyList => NEL }
scala> NEL(1)
```

`++` 演算に関して `NEL[A]` の semigroup が形成されていて欲しいところだけども、
デフォルトでは定義されていないため、`SemigroupK` から導き出す必要がある:

```console
scala> SemigroupK[NEL].algebra[String]
```

これで、`NEL[A]` を invalid 側に使って失敗値の蓄積を行うことができる:

```console
scala> val result = {
         implicit val nelSemigroup: Semigroup[NEL[String]] = SemigroupK[NEL].algebra[String]
         (valid[NEL[String], String]("event 1 ok") |@|
           invalid[NEL[String], String](NEL("event 2 failed!")) |@|
           invalid[NEL[String], String](NEL("event 3 failed!"))) map {_ + _ + _}
       }
```

`Invalid` の中に全ての失敗メッセージが入っている。

`fold` を使って値を取り出してみる:

```console
scala> val errs: NEL[String] = result.fold(
         { l => l },
         { r => sys.error("invalid is expected") }
       )
```

今日はここまで! 続きはまた今度。
