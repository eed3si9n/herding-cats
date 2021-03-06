
### Show

LYAHFGG:

> Members of `Show` can be presented as strings.

Cats' equivalent for the `Show` typeclass is `Show`:

```scala mdoc
import cats._, cats.syntax.all._

3.show

"hello".show
```

Here's the typeclass contract:

```scala
@typeclass trait Show[T] {
  def show(f: T): String
}
```

At first, it might seem silly to define `Show` because Scala
already has `toString` on `Any`.
`Any` also means anything would match the criteria, so you lose type safety.
The `toString` could be junk supplied by some parent class:

```scala mdoc
(new {}).toString
```

```scala mdoc:fail
(new {}).show
```

`object Show` provides two functions to create a `Show` instance:

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

Let's try using them:

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
