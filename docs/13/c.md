---
out: abstract-future.html
---

  [af]: http://logji.blogspot.com/2014/02/the-abstract-future.html

### The Abstract Future

One blog post that I occasionally see being mentioned as a poweful application of monad, especially in the context of building large application is [The Abstract Future][af]. It was originally posted to the precog.com engineering blog on November 27, 2012 by Kris Nuttycombe ([@nuttycom](https://twitter.com/nuttycom)).

>  At Precog, we use Futures extensively, both in a direct fashion and to allow us a composable way to interact with subsystems that are implemented atop Akka’s actor framework. Futures are arguably one of the best tools we have for reining in the complexity of asynchronous programming, and so our many of our early versions of APIs in our codebase exposed Futures directly.
> ....
>
> What this means is that from the perspective of the consumer of the DatasetModule interface, the only aspect of Future that we’re relying upon is the ability to order operations in a statically checked fashion; the sequencing, rather than any particular semantics related to Future’s asynchrony, is the relevant piece of information provided by the type. So, the following generalization becomes natural.

Here I'll use similar example as the Yoshida-san's.

```console:new
scala> import cats._, cats.instances.all._
scala> :paste
case class User(id: Long, name: String)

// In actual code, probably more than 2 errors
sealed trait Error
object Error {
  final case class UserNotFound(userId: Long) extends Error
  final case class ConnectionError(message: String) extends Error
}
trait UserRepos[F[_]] {
  implicit def F: Monad[F]
  def userRepo: UserRepo
  trait UserRepo {
    def followers(userId: Long): F[List[User]]
  }
}
```

#### UserRepos with Future

Let's start implementing the `UserRepos` module using `Future`.

```console
scala> :paste
import scala.concurrent.{ Future, ExecutionContext, Await }
import scala.concurrent.duration.Duration

class UserRepos0(implicit ec: ExecutionContext) extends UserRepos[Future] {
  override val F = implicitly[Monad[Future]]
  override val userRepo: UserRepo = new UserRepo0 {}
  trait UserRepo0 extends UserRepo {
    def followers(userId: Long): Future[List[User]] = Future.successful { Nil }
  }
}
```

Here's how to use it:

```console
scala> val service = new UserRepos0()(ExecutionContext.global)
scala> val xs = service.userRepo.followers(1L)
```

Now we have an asynchronous result. Let's say during testing we would like it to be synchronous.

#### UserRepos with Id

> In a test, we probably don’t want to worry about the fact that the computation is being performed asynchronously; all that we care about is that we obtain a correct result.
> ....
>
> For most cases, we’ll use the identity monad for testing. Suppose that we’re testing the piece of functionality described earlier, which has computed a result from the combination of a load, a sort, take and reduce. The test framework need never consider the monad that it’s operating in.

This is where Id datatype can be used.

```console
scala> :paste
class TestUserRepos extends UserRepos[Id] {
  override val F = implicitly[Monad[Id]]
  override val userRepo: UserRepo = new UserRepo0 {}
  trait UserRepo0 extends UserRepo {
    def followers(userId: Long): List[User] =
      userId match {
        case 0L => List(User(1, "Michael"))
        case 1L => List(User(0, "Vito"))
        case x =>  sys.error("not found")
      }
  }
}
```

Here's how to use it:


```console
scala> val testRepo = new TestUserRepos {}
scala> val ys = testRepo.userRepo.followers(1L)
```

#### Coding in abstract

Now that we were able to abtract the type constructor of the followers, let's try implementing `isFriends` from day 10 that checks if two users follow each other.

```console
scala> :paste
trait UserServices[F[_]] { this: UserRepos[F] =>
  def userService: UserService = new UserService
  class UserService {
    def isFriends(user1: Long, user2: Long): F[Boolean] =
      F.flatMap(userRepo.followers(user1)) { a =>
        F.map(userRepo.followers(user2)) { b =>
          a.exists(_.id == user2) && b.exists(_.id == user1)
        }
      }
  }
}
```

Here's how to use it:

```console
scala> val testService = new TestUserRepos with UserServices[Id] {}
scala> testService.userService.isFriends(0L, 1L)
```

The above demonstrates that `isFriends` can be written without knowing anything about `F[]` apart from the fact that it forms a `Monad`. It would be nice if I could use infix `flatMap` and `map` method while keeping `F` abstract. I tried creating `FlatMapOps(fa)` manually, but that resulted in abstract method error during runtime. The `actM` macro that we implemented on day 6 seems to work ok:

```console
scala> :paste
trait UserServices[F[_]] { this: UserRepos[F] =>
  def userService: UserService = new UserService
  class UserService {
    import example.MonadSyntax._
    def isFriends(user1: Long, user2: Long): F[Boolean] =
      actM[F, Boolean] {
        val a = userRepo.followers(user1).next
        val b = userRepo.followers(user2).next
        a.exists(_.id == user2) && b.exists(_.id == user1)
      }
  }
}
scala> val testService = new TestUserRepos with UserServices[Id] {}
scala> testService.userService.isFriends(0L, 1L)
```

#### UserRepos with EitherT

We can also use this with the `EitherT` with `Future` to carry a custom error type.

```console
scala> :paste
import cats.data.EitherT
class UserRepos1(implicit ec: ExecutionContext) extends UserRepos[EitherT[Future, Error, ?]] {
  override val F = implicitly[Monad[EitherT[Future, Error, ?]]]
  override val userRepo: UserRepo = new UserRepo1 {}
  trait UserRepo1 extends UserRepo {
    def followers(userId: Long): EitherT[Future, Error, List[User]] =
      userId match {
        case 0L => EitherT.right(Future { List(User(1, "Michael")) })
        case 1L => EitherT.right(Future { List(User(0, "Vito")) })
        case x =>
          EitherT.left(Future.successful { Error.UserNotFound(x) })
      }
  }
}
```

Here's how to use it:

```console
scala> val service1 = {
  import ExecutionContext.Implicits._
  new UserRepos1 with UserServices[EitherT[Future, Error, ?]] {}
}
scala> {
  import scala.concurrent.duration._
  Await.result(service1.userService.isFriends(0L, 1L).value, 1 second)
}
scala> {
  import scala.concurrent.duration._
  Await.result(service1.userService.isFriends(0L, 2L).value, 1 second)
}
```

Note that for all three versions of services, I was able to reuse the `UserServices` trait without any changes.

That's it for today.
