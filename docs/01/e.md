
### Show

LYAHFGG:

> Members of `Show` can be presented as strings.

Cats' equivalent for the `Show` typeclass is `Show`:

```console:new
scala> import cats._, cats.instances.all._, cats.syntax.show._
scala> 3.show
scala> "hello".show
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

```console:error
scala> (new {}).toString
scala> (new {}).show
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

```console
scala> :paste
case class Person(name: String)
case class Car(model: String)

scala> implicit val personShow = Show.show[Person](_.name)
scala> Person("Alice").show
scala> implicit val carShow = Show.fromToString[Car]
scala> Car("CR-V").show
```
