
### Show

LYAHFGG:

> ある値は、その値が `Show` 型クラスのインスタンスになっていれば、文字列として表現できます。

Cats で `Show` に対応する型クラスは `Show` だ:

```scala mdoc
import cats._, cats.data._, cats.implicits._

3.show

"hello".show
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

```scala mdoc
(new {}).toString
```

```scala mdoc:fail
(new {}).show
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

```scala mdoc
case class Person(name: String)
case class Car(model: String)

{
  implicit val personShow = Show.show[Person](_.name)
  Person("Alice").show
}

{
  implicit val carShow = Show.fromToString[Car]
  Car("CR-V")
}
```
