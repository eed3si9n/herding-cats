  [Apply]: Apply.html
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids

### Applicative

**注意**: アプリカティブ・ファンクターに興味があってこのページに飛んできた人は、まずは
[Apply][Apply] を読んでほしい。

[Functors, Applicative Functors and Monoids][fafm]:

> `Control.Applicative` モジュールにある型クラス `Applicative` に会いに行きましょう！型クラス `Applicative` は、2つの関数 `pure` と `<*>` を定義しています。

Cats の `Applicative` を見てみよう:

```scala
@typeclass trait Applicative[F[_]] extends Apply[F] { self =>
  /**
   * `pure` lifts any value into the Applicative Functor
   *
   * Applicative[Option].pure(10) = Some(10)
   */
  def pure[A](x: A): F[A]

  ....
}
```

`Apply` を拡張して `pure` をつけただけだ。

LYAHFGG:

> `pure` は任意の型の引数を受け取り、それをアプリカティブ値の中に入れて返します。 ... アプリカティブ値は「箱」というよりも「文脈」と考えるほうが正確かもしれません。`pure` は、値を引数に取り、その値を何らかのデフォルトの文脈（元の値を再現できるような最小限の文脈）に置くのです。

`A` の値を受け取り `F[A]` を返すコンストラクタみたいだ。

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> Applicative[List].pure(1)
scala> Applicative[Option].pure(1)
```

これは、`Apply[F].ap` を書くときに `{{...}.some}` としなくて済むのが便利かも。

```console
scala> val F = Applicative[Option]
scala> F.ap({ F.pure((_: Int) + 3) })(F.pure(9))
```

`Option` を抽象化したコードになった。

#### Applicative の便利な関数

LYAHFGG:

> では、「アプリカティブ値のリスト」を取って「リストを返り値として持つ1つのアプリカティブ値」を返す関数を実装してみましょう。これを `sequenceA` と呼ぶことにします。

```haskell
sequenceA :: (Applicative f) => [f a] -> f [a]  
sequenceA [] = pure []  
sequenceA (x:xs) = (:) <\$> x <*> sequenceA xs  
```

これを Cats でも実装できるか試してみよう!

```console
scala> def sequenceA[F[_]: Applicative, A](list: List[F[A]]): F[List[A]] = list match {
         case Nil     => Applicative[F].pure(Nil: List[A])
         case x :: xs => (x, sequenceA(xs)) mapN {_ :: _} 
       }
```

テストしてみよう:

```console
scala> sequenceA(List(1.some, 2.some))
scala> sequenceA(List(3.some, none[Int], 1.some))
scala> sequenceA(List(List(1, 2, 3), List(4, 5, 6)))
```

正しい答えが得られた。興味深いのは結局 `Applicative` が必要になったことと、
`sequenceA` が型クラスを利用したジェネリックな形になっていることだ。

> `sequenceA` は、関数のリストがあり、そのすべてに同じ引数を食わして結果をリストとして眺めたい、という場合にはとても便利です。

`Function1` の片側が `Int` に固定された例は、型解釈を付ける必要がある。

```console
scala> val f = sequenceA[Function1[Int, ?], Int](List((_: Int) + 3, (_: Int) + 2, (_: Int) + 1))
scala> f(3)
```

#### Applicative則

以下がの `Applicative` のための法則だ:

- identity: `pure id <*> v = v`
- homomorphism: `pure f <*> pure x = pure (f x)`
- interchange: `u <*> pure y = pure (\$ y) <*> u`

Cats はもう 1つ別の法則を定義している:

```scala
  def applicativeMap[A, B](fa: F[A], f: A => B): IsEq[F[B]] =
    fa.map(f) <-> fa.ap(F.pure(f))
```

`F.ap` と `F.pure` を合成したとき、それは `F.map` と同じ効果を得られるということみたいだ。

結構長くなったけど、ここまでたどり着けて良かったと思う。続きはまたあとで。
