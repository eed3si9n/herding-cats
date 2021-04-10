---
out: shape-and-contents.html
---

### 形とコンテンツ

EIP:

> 要素の収集に関してパラメトリックに多相であることの他に、
> このジェネリックな `traverse` 演算はもう 2つの次元によってパラメータ化されている:
> traverse されるデータ型と、traversal が解釈されるアプリカティブ・ファンクターだ。
> 後者をモノイドとしてのリストに特化すると、ジェネリックな `contents` 演算が得られる。

Cats を用いて実装するとこうなる:

```scala mdoc
import cats._, cats.data._, cats.syntax.all._

def contents[F[_], A](fa: F[A])(implicit FF: Traverse[F]): Const[List[A], F[Unit]] =
  {
    val contentsBody: A => Const[List[A], Unit] = { (a: A) => Const(List(a)) }
    FF.traverse(fa)(contentsBody)
  }
```

これで `Traverse` をサポートする任意のデータ型から `List` を得られるようになった。

```scala mdoc
contents(Vector(1, 2, 3)).getConst
```

これが逆順になっているのは果たして正しいのか、ちょっと定かではない。

> 分解の片方は、単純な写像 (map)、つまり恒等アプリカティブ・ファンクターによって解釈される traversal
> から得ることができる。

恒等アプリカティブ・ファンクターとは `Id[_]` のことだというのは既にみた通り。

```scala mdoc
def shape[F[_], A](fa: F[A])(implicit FF: Traverse[F]): Id[F[Unit]] =
  {
    val shapeBody: A => Id[Unit] = { (a: A) => () }
    FF.traverse(fa)(shapeBody)
  }
```

`Vector(1, 2, 3)` の形はこうなる:

```scala mdoc
shape(Vector(1, 2, 3))
```

EIP:

> この traversal のペアは、ここで取り上げている反復の 2つの側面、
> すなわち写像 (mapping) と累積 (accumulation) を体現するものとなっている。

次に、EIP はアプリカティブ合成を説明するために `shape` と `contents` を以下のように組み合わせている:

```scala mdoc
def decompose[F[_], A](fa: F[A])(implicit FF: Traverse[F]) =
  Tuple2K[Const[List[A], *], Id, F[Unit]](contents(fa), shape(fa))

val d = decompose(Vector(1, 2, 3))

d.first

d.second
```

問題は `traverse` が 2回走っていることだ。

> これら2つの走査 (traversal) を 1つに融合 (fuse) させることはできないだろうか?
> アプリカティブ・ファンクターの積は正にそのためにある。

これを `AppFunc` で書いてみよう。

```scala mdoc:reset:invisible
import cats._, cats.data._, cats.syntax.all._
```

```scala mdoc
import cats.data.Func.appFunc

def contentsBody[A]: AppFunc[Const[List[A], *], A, Unit] =
  appFunc[Const[List[A], *], A, Unit] { (a: A) => Const(List(a)) }

def shapeBody[A]: AppFunc[Id, A, Unit] =
  appFunc { (a: A) => ((): Id[Unit]) }

def decompose[F[_], A](fa: F[A])(implicit FF: Traverse[F]) =
  (contentsBody[A] product shapeBody[A]).traverse(fa)

val d = decompose(Vector(1, 2, 3))

d.first

d.second
```

`decompose`　の戻り値の型が少しごちゃごちゃしてきたが、`AppFunc` によって推論されている:
`Tuple2K[Const[List[Int], β1], Id[A], Vector[Unit]]`.
