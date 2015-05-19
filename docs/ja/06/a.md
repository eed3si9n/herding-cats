---
out: do-vs-for.html
---

  [SLS_6_19]: http://www.scala-lang.org/files/archive/spec/2.11/06-expressions.html#for-comprehensions-and-for-loops
  [foco]: http://docs.scala-lang.org/overviews/core/architecture-of-scala-collections.html#factoring-out-common-operations
  [focoja]: http://eed3si9n.github.io/scala-collections-impl-doc-ja/
  [ScalaAsync]: https://github.com/scala/async
  [Effectful]: https://github.com/pelotom/effectful
  [BasicDef]: http://www.scala-sbt.org/0.13/tutorial/Basic-Def.html
  [BasicDefJa]: http://www.scala-sbt.org/0.13/tutorial/ja/Basic-Def.html

### do vs for

Haskell の `do` 記法と Scala の `for` 内包表記には微妙な違いがある。以下が `do` 記法の例:

```haskell
foo = do
  x <- Just 3
  y <- Just "!"
  Just (show x ++ y)
```

通常は `return (show x ++ y)` と書くと思うけど、最後の行がモナディックな値であることを強調するために `Just` を書き出した。一方 Scala はこうだ:

```console
scala> def foo = for {
         x <- Some(3)
         y <- Some("!")
       } yield x.toString + y
```

似ているように見えるけども、いくつかの違いがある。

- Scala には標準で `Monad` 型が無い。その代わりにコンパイラが機械的に for 内容表記を `map`、 `withFilter`、 `flatMap`、 `foreach` の呼び出しに展開する。 [SLS 6.19][SLS_6_19]
- `Option` や `List` など、標準ライブラリが `map`/`flatMap` を実装するものは、Cats が提供する型クラスよりも組み込みの実装が優先される。
- Scala collection ライブラリの `map` その他は `F[A]` を `G[B]` に変換する `CanBuildFrom` を受け取る。[Scala コレクションのアーキテクチャ][focoja] 参照。
- `CanBuildFrom` は `G[A]` から `F[B]` という変換を行うこともある。
- pure 値を伴う `yield` を必要とする。さもないと、`for` は `Unit` を返す。

具体例を見てみよう:

```console
scala> import collection.immutable.BitSet
scala> val bits = BitSet(1, 2, 3)
scala> for {
         x <- bits
       } yield x.toFloat
scala> for {
         i <- List(1, 2, 3)
         j <- Some(1)
       } yield i + j
scala> for {
         i <- Map(1 -> 2)
         j <- Some(3)
       } yield j
```

#### actM を実装する

Scala には、マクロを使って命令型的なコードをモナディックもしくは applicative
な関数呼び出しに変換している DSL がいくつか既にある:

- [Scala Async][ScalaAsync]
- [Effectful][Effectful]
- [sbt 0.13 構文][BasicDefJa]

Scala 構文の全域をマクロでカバーするのは難しい作業だけども、
Async と Effectful のコードをコピペすることで単純な式と `val`
のみをサポートするオモチャマクロを作ってみた。
詳細は省くが、ポイントは以下の関数だ:

```scala
  def transform(group: BindGroup, isPure: Boolean): Tree =
    group match {
      case (binds, tree) =>
        binds match {
          case Nil =>
            if (isPure) q"""\$monadInstance.pure(\$tree)"""
            else tree
          case (name, unwrappedFrom) :: xs =>
            val innerTree = transform((xs, tree), isPure)
            val param = ValDef(Modifiers(Flag.PARAM), name, TypeTree(), EmptyTree)
            q"""\$monadInstance.flatMap(\$unwrappedFrom) { \$param => \$innerTree }"""
        }
    }
```

`actM` を使ってみよう:

```console
scala> import cats._, cats.std.all._
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> import example.MonadSyntax._
scala> actM[Option, String] {
         val x = 3.some.next
         val y = "!".some.next
         x.toString + y
       }
```

`fa.next` は `Monad[F].flatMap(fa)()` の呼び出しに展開される。
そのため、上の例はこのように展開される:

```console
scala> Monad[Option].flatMap[String, String]({
         val fa0: Option[Int] = 3.some
         Monad[Option].flatMap[Int, String](fa0) { (arg0: Int) => {
           val next0: Int = arg0
           val x: Int = next0
           val fa1: Option[String] = "!".some
           Monad[Option].flatMap[String, String](fa1)((arg1: String) => {
             val next1: String = arg1
             val y: String = next1
             Monad[Option].pure[String](x.toString + y)
           })
         }}
       }) { (arg2: String) => Monad[Option].pure[String](arg2) }
```

`Option` から `List` への自動変換を防止できるか試してみる:

```console
scala> actM[List, Int] {
         val i = List(1, 2, 3).next
         val j = 1.some.next
         i + j
       }
```

エラーメッセージがこなれないけども、コンパイル時に検知することができた。
これは、`Future` を含むどのモナドでも動作する。

```console
scala> :paste
val x = {
  import scala.concurrent.{ExecutionContext, Future}
  import ExecutionContext.Implicits.global
  actM[Future, Int] {
    val i = Future { 1 }.next
    val j = Future { 2 }.next
    i + j
  }
}
scala> x.value
```

このマクロは不完全な toy code だけども、こういうものがあれば便利なのではという示唆はできたと思う。
