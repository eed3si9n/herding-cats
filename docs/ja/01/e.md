
### Show

LYAHFGG:

> ある値は、その値が `Show` 型クラスのインスタンスになっていれば、文字列として表現できます。

Cats で `Show` に対応する型クラスは `Show` だ:

```console:new
scala> import cats._, cats.std.all._, cats.syntax.show._
scala> 3.show
scala> "hello".show
```

これが型クラスのコントラクトだ:

```scala
@typeclass trait Show[T] {
  def show(f: T): String
}
```

Scala には既に `Any` に `toString` があるため、`Show`
を定義するのは馬鹿げているように一見見えるかもしれない。
`Any` ということは逆に何でも該当してしまうので、型安全性を失うことになる。
`toString` は何らかの親クラスが書いたゴミかもしれない:

```console:error
scala> (new {}).toString
scala> (new {}).show
```

`object Show` は `Show` のインスタンスを作成するための 2つの関数を提供する:

```scala
object Show {
  /** creates an instance of [[Show]] using the provided function */
  def show[A](f: A => String): Show[A] = new Show[A] {
    def show(a: A): String = f(a)
  }

  /** creates an instance of [[Show]] using object toString */
  def fromToString[A]: Show[A] = new Show[A] {
    def show(a: A): String = a.toString
  }

  implicit val catsContravariantForShow: Contravariant[Show] = new Contravariant[Show] {
    def contramap[A, B](fa: Show[A])(f: B => A): Show[B] =
      show[B](fa.show _ compose f)
  }
}
```

使ってみる:

```console
scala> :paste
case class Person(name: String)
case class Car(model: String)

scala> implicit val personShow = Show.show[Person](_.name)
scala> Person("Alice").show
scala> implicit val carShow = Show.fromToString[Car]
scala> Car("CR-V")
```
