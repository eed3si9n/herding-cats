---
out: Unapply.html
---

  [SI-2712]: https://issues.scala-lang.org/browse/SI-2712
  [cecc3a]: https://github.com/stew/cats/commit/cecc3afbdbb6fbbe764005cd52e9efe7acdfc8f2
  [combining-applicative]: combining-applicative.html

### Unapply を用いた型推論の強制

EIP:

> ここではいくつかのデータ型とそれに関連した強要関数 (coercion function)、
> `Id`、 `unId`、 `Const`、 `unConst` が出てくる。
> 読みやすくするために、これらの強要に共通する記法を導入する。

Scala の場合は implicit と型推論だけで結構いける。
だけど、型クラスを駆使していると Scala の型推論の弱点にも出くわすことがある。
中でも頻繁に遭遇するのは部分適用されたパラメータ型を推論できないという問題で、
[SI-2712][SI-2712] として知られている。
今、これを読んでいるならば、そのページに飛んで投票を行うか、できれば問題を解決するのを手伝ってきてほしい。

#### Unapply

この問題の回避策として Cats は `Unapply` と呼ばれる型クラスを使う:

```scala
/**
 * A typeclass that is used to help guide scala's type inference to
 * find typeclass instances for types which have shapes which differ
 * from what their typeclasses are looking for.
 *
 * For example, [[Functor]] is defined for types in the shape
 * F[_]. Scala has no problem finding instance of Functor which match
 * this shape, such as Functor[Option], Functor[List], etc. There is
 * also a functor defined for some types which have the Shape F[_,_]
 * when one of the two 'holes' is fixed. For example. there is a
 * Functor for Map[A,?] for any A, and for Either[A,?] for any A,
 * however the scala compiler will not find them without some coercing.
 */
trait Unapply[TC[_[_]], MA] {
  // a type constructor which is properly kinded for the typeclass
  type M[_]
  // the type applied to the type constructor to make an MA
  type A

  // the actual typeclass instance found
  def TC: TC[M]

  // a function which will coerce the MA value into one of type M[A]
  // this will end up being the identity function, but we can't supply
  // it until we have proven that MA and M[A] are the same type
  def subst: MA => M[A]
}
```

具体例を用いて説明した方が早いと思う。

```console:new
scala> import cats._, cats.std.all._
scala> def foo[F[_]: Applicative](fa: F[Int]): F[Int] = fa
```

上の例では、`foo` は渡された値 `fa: F[Int]` を返すだけの簡単な関数だが、
ただし、`F` は `Applicative` を形成することとする。
`Either[String, Int]` はアプリカティブであるので、条件を満たすはずだ。

```console
scala> foo(Right(1): Either[String, Int])
```

フォーマルなパラメータ型と引数の式の型に互換性が無いというエラーが出た。
`Unapply` 版はこのように書ける:

```console
scala> def fooU[FA](fa: FA)(implicit U: Unapply[Applicative, FA]): U.M[U.A] =
         U.subst(fa)
```

試したのと同一のパラメータを渡してみる:

```console
scala> fooU(Right(1): Either[String, Int])
```

うまくいった。どのように実装されているか見てみよう。
`Either` の場合、モナドは `Either[AA, ?]` に対して形成されているため、
`List[Int]` のように `map` の前後で右側のパラメータは変わるかもしれないが、
左側は固定されている。

```scala
sealed abstract class Unapply2Instances extends Unapply3Instances {
  type Aux2Right[TC[_[_]], MA, F[_,_], AA, B] = Unapply[TC, MA] {
    type M[X] = F[AA,X]
    type A = B
  }

  implicit def unapply2right[TC[_[_]], F[_,_], AA, B](implicit tc: TC[F[AA,?]]): Aux2Right[TC,F[AA,B], F, AA, B] = new Unapply[TC, F[AA,B]] {
     type M[X] = F[AA, X]
     type A = B
     def TC: TC[F[AA, ?]] = tc
     def subst: F[AA, B] => M[A] = identity
   }

   ....
}
```

まず Cats は抽象型の `M[_]` と `A` を定義する型エイリアスである `Aux2Right` を定義する。
次に、任意の型クラス・インスタンス `TC[F[AA,?]]` から `Aux2Right[TC,F[AA,B], F, AA, B]`
への暗黙の変換を定義する。

#### インスタンスのデコ化

Cats で `Unapply` が出てくる箇所としていわゆる「syntax」と呼ばれる中置記法の注入が挙げられる。
これらの暗黙の変換は [cecc3a][cecc3a] において「bedazzler」(ラインストーンを取り付けるためのデコ化器具) と呼ばれている。

```scala
package cats
package syntax

trait FunctorSyntax1 {
  implicit def functorSyntaxU[FA](fa: FA)(implicit U: Unapply[Functor,FA]): Functor.Ops[U.M, U.A] =
    new Functor.Ops[U.M, U.A]{
      val self = U.subst(fa)
      val typeClassInstance = U.TC
    }
}

trait FunctorSyntax extends Functor.ToFunctorOps with FunctorSyntax1
```

`Apply` の　`*>` 演算子を使ってみる:

```console
scala> import cats.syntax.apply._
scala> (Right(1): Either[String, Int]) *> Right(2)
```

これが動作したのは、多分 `Unapply` のお陰だと思う。

#### 限界

11日目にみた [AppFunc][combining-applicative] を使うことでより複雑な
合成アプリカティブ・ファンクターインスタンスを作ることができるようになった。
これを使って嬉しいのはインスタンスを自動で導き出してくれることなので、
`Unapply` が無ければその便利さは半減するだろう。
しかし、同時に、`Unapply` は全ての形を事前に知っている必要があり、
現状で最も複雑な形は `F[AA, B, ?]` なので、最終的な解とはなり得ない。
2つのパラメータを受け取るモナドインスタンスを合成して、その積を作るだけで簡単にこれを超えた形を作ることができる。

飽くまで憶測にすぎないが、この型推論の問題は Scala コンパイラそのものが
逐次的合成と並列合成 (積) を型システムの一級市民として取り扱わないと解決しない問題なんじゃないだろうか。
