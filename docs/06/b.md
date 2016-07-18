---
out: Reader.html
---

  [@runarorama]: https://twitter.com/runarorama
  [@jarhart]: https://twitter.com/jarhart
  [dsdi]: http://functionaltalks.org/2013/06/17/runar-oli-bjarnason-dead-simple-dependency-injection/
  [ltudif]: https://yow.eventer.com/yow-2012-1012/lambda-the-ultimate-dependency-injection-framework-by-runar-bjarnason-1277
  [sycpb]: http://blog.originate.com/blog/2013/10/21/reader-monad-for-dependency-injection/
  [fafmm]: http://learnyouahaskell.com/for-a-few-monads-more

### Reader datatype

[Learn You a Haskell for Great Good][fafmm] says:

> In the chapter about applicatives, we saw that the function type, `(->) r` is an instance of `Functor`.

```console:new
scala> import cats._, cats.std.all._, cats.syntax.functor._
scala> val f = (_: Int) * 2
scala> val g = (_: Int) + 10
scala> (g map f)(8)
```

> We've also seen that functions are applicative functors. They allow us to operate on the eventual results of functions as if we already had their results.

```console
scala> import cats.syntax.cartesian._
scala> val h = (f |@| g) map {_ + _}
scala> h(3)
```

> Not only is the function type `(->) r a` functor and an applicative functor, but it's also a monad. Just like other monadic values that we've met so far, a function can also be considered a value with a context. The context for functions is that that value is not present yet and that we have to apply that function to something in order to get its result value.

Let's try implementing the example:

```console
scala> import cats.syntax.flatMap._
scala> val addStuff: Int => Int = for {
         a <- (_: Int) * 2
         b <- (_: Int) + 10
       } yield a + b
scala> addStuff(3)
```

> Both `(*2)` and `(+10)` get applied to the number `3` in this case. `return (a+b)` does as well, but it ignores it and always presents `a+b` as the result. For this reason, the function monad is also called the *reader* monad. All the functions read from a common source.

The `Reader` monad lets us pretend the value is already there. I am guessing that this works only for functions that accepts one parameter.

#### Dependency injection

At nescala 2012 on March 9th, Rúnar ([@runarorama][@runarorama]) gave a talk [Dead-Simple Dependency Injection][dsdi].
One of the ideas presented there was to use the `Reader` monad for dependency injection. Later that year, he also gave a longer version of the talk [Lambda: The Ultimate Dependency Injection Framework][ltudif] in YOW 2012.
In 2013, Jason Arhart wrote [Scrap Your Cake Pattern Boilerplate: Dependency Injection Using the Reader Monad][sycpb],
which I'm going to base my example on.

Imagine we have a case class for a user, and a trait that abstracts the data store to get them.

```console
scala> :paste
case class User(id: Long, parentId: Long, name: String, email: String)
trait UserRepo {
  def get(id: Long): User
  def find(name: String): User
}
```

Next we define a primitive reader for each operation defined in the `UserRepo` trait:

```console
scala> :paste
trait Users {
  def getUser(id: Long): UserRepo => User = {
    case repo => repo.get(id)
  }
  def findUser(name: String): UserRepo => User = {
    case repo => repo.find(name)
  }
}
```

That looks like boilerplate. (I thought we are scrapping it.) Moving on.

Based on the primitive readers, we can compose other readers,
including the application.

```console
scala> :paste
object UserInfo extends Users {
  def userInfo(name: String): UserRepo => Map[String, String] =
    for {
      user <- findUser(name)
      boss <- getUser(user.parentId)
    } yield Map(
      "name" -> s"\${user.name}",
      "email" -> s"\${user.email}",
      "boss_name" -> s"\${boss.name}"
    )
}
trait Program {
  def app: UserRepo => String =
    for {
      fredo <- UserInfo.userInfo("Fredo")
    } yield fredo.toString
}
```

To run this `app`, we need something that provides an implementation for `UserRepo`:

```console
scala> :paste
import cats.syntax.eq._

val testUsers = List(User(0, 0, "Vito", "vito@example.com"),
  User(1, 0, "Michael", "michael@example.com"),
  User(2, 0, "Fredo", "fredo@example.com"))
object Main extends Program {
  def run: String = app(mkUserRepo)
  def mkUserRepo: UserRepo = new UserRepo {
    def get(id: Long): User = (testUsers find { _.id === id }).get
    def find(name: String): User = (testUsers find { _.name === name }).get
  }
}
Main.run
```

We got the boss man's name.

We can try using `actM` instead of a `for` comprehension:

```console
scala> :paste
object UserInfo extends Users {
  import example.MonadSyntax._
  def userInfo(name: String): UserRepo => Map[String, String] =
    actM[UserRepo => ?, Map[String, String]] {
      val user = findUser(name).next
      val boss = getUser(user.parentId).next
      Map(
        "name" -> s"\${user.name}",
        "email" -> s"\${user.email}",
        "boss_name" -> s"\${boss.name}"
      )
    }
}
trait Program {
  import example.MonadSyntax._
  def app: UserRepo => String =
    actM[UserRepo => ?, String] {
      val fredo = UserInfo.userInfo("Fredo").next
      fredo.toString
    }
}
object Main extends Program {
  def run: String = app(mkUserRepo)
  def mkUserRepo: UserRepo = new UserRepo {
    def get(id: Long): User = (testUsers find { _.id === id }).get
    def find(name: String): User = (testUsers find { _.name === name }).get
  }
}
Main.run
```

The code inside of the `actM` block looks more natural than the `for` version,
but the type annotations probably make it more difficult to use.

That's all for today.
