---
out: monad-transfomers.html
---

  [mf]: http://book.realworldhaskell.org/read/monad-transformers.html
  [Reader]: Reader.html
  [sycpb]: http://blog.originate.com/blog/2013/10/21/reader-monad-for-dependency-injection/
  [Free-monads]: Free-monads.html

One topic we have been dancing around, but haven't gotten into is the notion of the monad transformer.
Luckily there's another good Haskell book that I've read that's also available online.

### Monad transformers

[Real World Haskell][mf] says:

> It would be ideal if we could somehow take the standard `State` monad and add failure handling to it, without resorting to the wholesale construction of custom monads by hand. The standard monads in the `mtl` library don't allow us to combine them. Instead, the library provides a set of *monad transformers* to achieve the same result.
>
> A monad transformer is similar to a regular monad, but it's not a standalone entity: instead, it modifies the behaviour of an underlying monad.

#### Dependency injection again

Let's look into the idea of using `Reader` datatype (`Function1`)
for dependency injection, which we saw on [day 6][Reader].

```console:new
scala> :paste
case class User(id: Long, parentId: Long, name: String, email: String)
trait UserRepo {
  def get(id: Long): User
  def find(name: String): User
}
```

Jason Arhart's [Scrap Your Cake Pattern Boilerplate: Dependency Injection Using the Reader Monad][sycpb] generalizes the notion of `Reader` datatype for supporting multiple services by creating a `Config` object:

```console
scala> import java.net.URI
scala> :paste
trait HttpService {
  def get(uri: URI): String
}
trait Config {
  def userRepo: UserRepo
  def httpService: HttpService
}
```

To use this, we would construct mini-programs of type `Config => A`, and compose them.

Suppose we want to also encode the notion of failure using `Option`.

#### Kleisli as ReaderT

We can use the `Kleisli` datatype we saw yesterday as `ReaderT`, a monad transformer version of the `Reader` datatype, and stack it on top of `Option` like this:

```console
scala> import cats._, cats.std.all._
scala> import cats.data.Kleisli, Kleisli.function
scala> :paste
type ReaderTOption[A, B] = Kleisli[Option, A, B]
object ReaderTOption {
  def ro[A, B](f: A => Option[B]): ReaderTOption[A, B] = function(f)
}
```

We can modify the `Config` to make `httpService` optional:

```console
scala> :paste
trait UserRepo {
  def get(id: Long): Option[User]
  def find(name: String): Option[User]
}
trait Config {
  def userRepo: UserRepo
  def httpService: Option[HttpService]
}
```

Next we can rewrtie the "primitive" readers to return `ReaderTOption[Config, A]`:

```console
scala> :paste
trait Users {
  def getUser(id: Long): ReaderTOption[Config, User] =
    ReaderTOption.ro {
      case config => config.userRepo.get(id)
    }
  def findUser(name: String): ReaderTOption[Config, User] =
    ReaderTOption.ro {
      case config => config.userRepo.find(name)
    }
}
trait Https {
  def getHttp(uri: URI): ReaderTOption[Config, String] =
    ReaderTOption.ro {
      case config => config.httpService map {_.get(uri)}
    }
}
```

We can compose these mini-programs into compound programs:

```console
scala> :paste
import cats.syntax.eq._
trait Program extends Users with Https {
  def userSearch(id: Long): ReaderTOption[Config, String] =
    for {
      u <- getUser(id)
      r <- getHttp(new URI(s"http://www.google.com/?q=\${u.name}"))
    } yield r
}
object Main extends Program {
  def run(config: Config): Option[String] =
    userSearch(2).run(config)
}
val dummyConfig: Config = new Config {
  val testUsers = List(User(0, 0, "Vito", "vito@example.com"),
    User(1, 0, "Michael", "michael@example.com"),
    User(2, 0, "Fredo", "fredo@example.com"))
  def userRepo: UserRepo = new UserRepo {
    def get(id: Long): Option[User] =
      testUsers find { _.id === id }
    def find(name: String): Option[User] =
      testUsers find { _.name === name }
  }
  def httpService: Option[HttpService] = None
}
```

The above `ReaderTOption` datatype combines `Reader`’s ability to read from some configuration once, and the `Option`’s ability to express failure.

### Stacking multiple monad transformers

RWH:

> When we stack a monad transformer on a normal monad, the result is another monad. This suggests the possibility that we can again stack a monad transformer on top of our combined monad, to give a new monad, and in fact this is a common thing to do.

We can stack `StateT` on top of `ReaderTOption` to represent state transfer.

```console
scala> import cats.state._
scala> :paste
type StateTReaderTOption[C, S, A] = StateT[({type l[X] = ReaderTOption[C, X]})#l, S, A]
object StateTReaderTOption {
  def state[C, S, A](f: S => (S, A)): StateTReaderTOption[C, S, A] =
    StateT[({type l[X] = ReaderTOption[C, X]})#l, S, A] {
      s: S => Monad[({type l[X] = ReaderTOption[C, X]})#l].pure(f(s))
    }
  def get[C, S]: StateTReaderTOption[C, S, S] =
    state { s => (s, s) }
  def put[C, S](s: S): StateTReaderTOption[C, S, Unit] =
    state { _ => (s, ()) }
  def ro[C, S, A](f: C => Option[A]): StateTReaderTOption[C, S, A] =
    StateT[({type l[X] = ReaderTOption[C, X]})#l, S, A] {
      s: S =>
        ReaderTOption.ro[C, (S, A)]{
          c: C => f(c) map {(s, _)}
        }
    }
}
```

This is a bit confusing, so let's break it down. Ultimately the point of `State` datatype is to wrap `S => (S, A)`, so I kept those parameter names for `state`. Next, I needed to modify the kind of `ReaderTOption` to `* -> *` (a type constructor that takes exactly one type as its parameter).

Similarly, we need a way of using this datatype as a `ReaderTOption`, which is expressed as `C => Option[A]` in `ro`.

We also can implement a `Stack` again. This time let's use `String` instead.

```console
scala> type Stack = List[String]
scala> val pop = StateTReaderTOption.state[Config, Stack, String] {
         case x :: xs => (xs, x)
         case _       => ???
       }
```

Here's a version of `pop` and `push` using `get` and `put` primitive:

```console
scala> import StateTReaderTOption.{get, put}
scala> val pop: StateTReaderTOption[Config, Stack, String] =
         for {
           s <- get[Config, Stack]
           (x :: xs) = s
           _ <- put(xs)
         } yield x
scala> def push(x: String): StateTReaderTOption[Config, Stack, Unit] =
         for {
           xs <- get[Config, Stack]
           r <- put(x :: xs)
         } yield r
```

We can also port `stackManip`:

```console
scala> def stackManip: StateTReaderTOption[Config, Stack, String] =
         for {
           _ <- push("Fredo")
           a <- pop
           b <- pop
         } yield(b)
```

Here's how we can use this:

```console
scala> stackManip.run(List("Hyman Roth")).run(dummyConfig)
```

So far we have the same feature as the `State` version.
We can modify `Users` to use `StateTReaderTOption.ro`:

```console
scala> :paste
trait Users {
  def getUser[S](id: Long): StateTReaderTOption[Config, S, User] =
    StateTReaderTOption.ro[Config, S, User] {
      case config => config.userRepo.get(id)
    }
  def findUser[S](name: String): StateTReaderTOption[Config, S, User] =
    StateTReaderTOption.ro[Config, S, User] {
      case config => config.userRepo.find(name)
    }
}
```

Using this we can now manipulate the stack using the read-only configuration:

```console
scala> :paste
trait Program extends Users {
  def stackManip: StateTReaderTOption[Config, Stack, Unit] =
    for {
      u <- getUser(2)
      a <- push(u.name)
    } yield(a)
}
object Main extends Program {
  def run(s: Stack, config: Config): Option[(Stack, Unit)] =
    stackManip.run(s).run(config)
}
```

We can run this program like this:

```console
scala> Main.run(List("Hyman Roth"), dummyConfig)
```

Now we have `StateT`, `ReaderT` and `Option` working all at the same time.
Maybe I am not doing it right, but setting up the monad constructor functions like `state` and `ro` to set up `StateTReaderTOption` is a rather mind-bending excercise.

Once the primitive monadic values are constructed, the usage code (like `stackManip`) looks relatively clean.
It sure does avoid the cake pattern, but the stacked monad type `StateTReaderTOption` is sprinkled all over the code base.

If all we wanted was being able to use `getUser(id: Long)` and `push` etc.,
an alternative is to construct a DSL with those commands using [Free monads][Free-monads] we saw on day 8.
