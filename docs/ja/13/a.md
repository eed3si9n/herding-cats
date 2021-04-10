---
out: Id.html
---

### Id データ型

EIP を読んでる途中でちらっと `Id` というものが出てきたけど、面白い道具なので、ちょっとみてみよう。
別名 Identiy、恒等射 (Identity functor)、恒等モナド (Identity monad) など文脈によって色んな名前で出てくる。
このデータ型の定義は非常にシンプルなものだ:

```scala
  type Id[A] = A
```

scaladoc と型クラスのインスタンスと一緒だとこうなっている:

```scala
  /**
   * Identity, encoded as `type Id[A] = A`, a convenient alias to make
   * identity instances well-kinded.
   *
   * The identity monad can be seen as the ambient monad that encodes
   * the effect of having no effect. It is ambient in the sense that
   * plain pure values are values of `Id`.
   *
   * For instance, the [[cats.Functor]] instance for `[[cats.Id]]`
   * allows us to apply a function `A => B` to an `Id[A]` and get an
   * `Id[B]`. However, an `Id[A]` is the same as `A`, so all we're doing
   * is applying a pure function of type `A => B` to a pure value  of
   * type `A` to get a pure value of type `B`. That is, the instance
   * encodes pure unary function application.
   */
  type Id[A] = A

  implicit val catsInstancesForId
    : Bimonad[Id] with CommutativeMonad[Id] with Comonad[Id] with NonEmptyTraverse[Id] with Distributive[Id] =
    new Bimonad[Id] with CommutativeMonad[Id] with Comonad[Id] with NonEmptyTraverse[Id] with Distributive[Id] {
      def pure[A](a: A): A = a
      def extract[A](a: A): A = a
      def flatMap[A, B](a: A)(f: A => B): B = f(a)
      def coflatMap[A, B](a: A)(f: A => B): B = f(a)
      @tailrec def tailRecM[A, B](a: A)(f: A => Either[A, B]): B =
        f(a) match {
          case Left(a1) => tailRecM(a1)(f)
          case Right(b) => b
        }
      override def distribute[F[_], A, B](fa: F[A])(f: A => B)(implicit F: Functor[F]): Id[F[B]] = F.map(fa)(f)
      override def map[A, B](fa: A)(f: A => B): B = f(fa)
      override def ap[A, B](ff: A => B)(fa: A): B = ff(fa)
      override def flatten[A](ffa: A): A = ffa
      override def map2[A, B, Z](fa: A, fb: B)(f: (A, B) => Z): Z = f(fa, fb)
      override def lift[A, B](f: A => B): A => B = f
      override def imap[A, B](fa: A)(f: A => B)(fi: B => A): B = f(fa)
      def foldLeft[A, B](a: A, b: B)(f: (B, A) => B) = f(b, a)
      def foldRight[A, B](a: A, lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
        f(a, lb)
      def nonEmptyTraverse[G[_], A, B](a: A)(f: A => G[B])(implicit G: Apply[G]): G[B] =
        f(a)
      override def foldMap[A, B](fa: Id[A])(f: A => B)(implicit B: Monoid[B]): B = f(fa)
      override def reduce[A](fa: Id[A])(implicit A: Semigroup[A]): A =
        fa
      def reduceLeftTo[A, B](fa: Id[A])(f: A => B)(g: (B, A) => B): B =
        f(fa)
      override def reduceLeft[A](fa: Id[A])(f: (A, A) => A): A =
        fa
      override def reduceLeftToOption[A, B](fa: Id[A])(f: A => B)(g: (B, A) => B): Option[B] =
        Some(f(fa))
      override def reduceRight[A](fa: Id[A])(f: (A, Eval[A]) => Eval[A]): Eval[A] =
        Now(fa)
      def reduceRightTo[A, B](fa: Id[A])(f: A => B)(g: (A, Eval[B]) => Eval[B]): Eval[B] =
        Now(f(fa))
      override def reduceRightToOption[A, B](fa: Id[A])(f: A => B)(g: (A, Eval[B]) => Eval[B]): Eval[Option[B]] =
        Now(Some(f(fa)))
      override def reduceMap[A, B](fa: Id[A])(f: A => B)(implicit B: Semigroup[B]): B = f(fa)
      override def size[A](fa: Id[A]): Long = 1L
      override def get[A](fa: Id[A])(idx: Long): Option[A] =
        if (idx == 0L) Some(fa) else None
      override def isEmpty[A](fa: Id[A]): Boolean = false
    }
```

`Id` の値はこのように作成する:

```scala mdoc
import cats._, cats.syntax.all._

val one: Id[Int] = 1
```

#### Functor としての Id

`Id` の `Functor` インスタンスは関数の適用と同じだ:

```scala mdoc
Functor[Id].map(one) { _ + 1 }
```

#### Apply としての Id

`Apply` の `ap` メソッドは `Id[A => B]` を受け取るが、実際にはただの `A => B` なので、これも関数適用として実装されている:

```scala mdoc
Apply[Id].ap({ _ + 1 }: Id[Int => Int])(one)
```

#### FlatMap としての Id

`FlatMap` の `flatMap` メソッドは `A => Id[B]` も同様。これも関数適用として実装されている:

```scala mdoc
FlatMap[Id].flatMap(one) { _ + 1 }
```

#### Id ってなんで嬉しいの?

一見 `Id` はあんまり便利そうじゃない。ヒントは定義の上にあった Scaladoc にある「恒等インスタンスのカインドを整えるための便利エイリアス」。つまり、なんらかの型 `A` を `F[A]` に持ち上げる必要があって、そのときに `Id` は作用を一切導入せずに使うことができる。あとでその例もみてみる。
