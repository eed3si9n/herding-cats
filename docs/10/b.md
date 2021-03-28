---
out: stacking-future-and-either.html
---

  [xuweik]: https://twitter.com/xuwei_k
  [combine-future-and-either]: http://d.hatena.ne.jp/xuwei/20140919/1411136788
  [seal]: https://twitter.com/xuwei_k/status/392260189673373696
  [xw]: https://en.wikipedia.org/wiki/Xu_Wei
  [3hj86e]: http://twitpic.com/3hj86e
  [Xor]: Xor.html
  [XorTSource]: $catsBaseUrl$core/src/main/scala/cats/data/XorT.scala

### Stacking Future and Either

One use of monad transformers that seem to come up often is stacking `Future` datatype with `Either`. There's a blog post by Yoshida-san ([@xuwei_k][xuweik]) in Japanese called [How to combine Future and Either nicely in Scala][combine-future-and-either].

A little known fact about Yoshida-san outside of Tokyo, is that he majored in Chinese calligraphy. Apparently he spent his college days writing ancient seal scripts and carving seals:

<blockquote class="twitter-tweet" lang="en"><p lang="ja" dir="ltr">「大学では、はんこを刻ったり、篆書を書いてました」&#10;「えっ？なぜプログラマに？？？」 <a href="http://t.co/DEhqy4ELpF">pic.twitter.com/DEhqy4ELpF</a></p>&mdash; Kenji Yoshida (@xuwei_k) <a href="https://twitter.com/xuwei_k/status/392260189673373696">October 21, 2013</a></blockquote>
<script async src="//platform.twitter.com/widgets.js" charset="utf-8"></script>

His namesake [Xu Wei][xw] was a Ming-era painter, poet, writer and dramatist famed for his free-flowing style. Here's Yoshida-san writing [functional programming language][3hj86e].

In any case, why would one want to stack `Future` and `Either` together?
The blog post explains like this:

1. `Future[A]` comes up a lot in Scala.
2. You don't want to block future, so you end up with `Future` everywhere.
3. Since `Future` is asynchronous, any errors need to be captured in there.
4. `Future` is designed to handle `Throwable`, but that's all you get.
5. When the program becomes complex enough, you want to type your own error states.
6. How can we make `Future` of `Either`?

Here's the prepration step:

```scala mdoc
case class User(id: Long, name: String)

// In actual code, probably more than 2 errors
sealed trait Error

object Error {
  final case class UserNotFound(userId: Long) extends Error
  final case class ConnectionError(message: String) extends Error
}

object UserRepo {
  def followers(userId: Long): Either[Error, List[User]] = ???
}

import UserRepo.followers
```

Suppose our application allows the users to follow each other like Twitter.
The `followers` function returns the list of followers.

Now let's try writing a function that checks if two users follow each other.

```scala mdoc
def isFriends0(user1: Long, user2: Long): Either[Error, Boolean] =
  for {
    a <- followers(user1).right
    b <- followers(user2).right
  } yield a.exists(_.id == user2) && b.exists(_.id == user1)
```

Now suppose we want to make the database access async
so we changed the `followers` to return a `Future` like this:

```scala mdoc:reset:invisible
case class User(id: Long, name: String)
sealed trait Error
object Error {
  final case class UserNotFound(userId: Long) extends Error
  final case class ConnectionError(message: String) extends Error
}
```

```scala mdoc
import scala.concurrent.{ Future, ExecutionContext }
object UserRepo {
  def followers(userId: Long): Future[Either[Error, List[User]]] = ???
}
import UserRepo.followers
```

Now, how would `isFriends0` look like? Here's one way of writing this:

```scala mdoc
def isFriends1(user1: Long, user2: Long)
  (implicit ec: ExecutionContext): Future[Either[Error, Boolean]] =
  for {
    a <- followers(user1)
    b <- followers(user2)
  } yield for {
    x <- a.right
    y <- b.right
  } yield x.exists(_.id == user2) && y.exists(_.id == user1)
```

And here's another version:

```scala mdoc
def isFriends2(user1: Long, user2: Long)
  (implicit ec: ExecutionContext): Future[Either[Error, Boolean]] =
  followers(user1) flatMap {
    case Right(a) =>
      followers(user2) map {
        case Right(b) =>
          Right(a.exists(_.id == user2) && b.exists(_.id == user1))
        case Left(e) =>
          Left(e)
      }
    case Left(e) =>
      Future.successful(Left(e))
  }
```

What is the difference between the two versions?
They'll both behave the same if `followers` return normally,
but suppose `followers(user1)` returns an `Error` state.
`isFriend1` would still call `followers(user2)`, whereas `isFriend2` would short-circuit and return the error.

Regardless, both functions became convoluted compared to the original.
And it's mostly a boilerplate to satisfy the types.
I don't want to imagine doing this for *every* function that uses `Future[Either[Error, A]]`.

#### EitherT datatype

Cats comes with `EitherT` datatype, which is a monad transformer for `Either`.

```scala
/**
 * Transformer for `Either`, allowing the effect of an arbitrary type constructor `F` to be combined with the
 * fail-fast effect of `Either`.
 *
 * `EitherT[F, A, B]` wraps a value of type `F[Either[A, B]]`. An `F[C]` can be lifted in to `EitherT[F, A, C]` via `EitherT.right`,
 * and lifted in to a `EitherT[F, C, B]` via `EitherT.left`.
 */
case class EitherT[F[_], A, B](value: F[Either[A, B]]) {
  ....
}
```

Here's `UserRepo.followers` with a dummy implementation:

```scala mdoc:reset:invisible
import scala.concurrent.{ Future, ExecutionContext }
case class User(id: Long, name: String)
sealed trait Error
object Error {
  final case class UserNotFound(userId: Long) extends Error
  final case class ConnectionError(message: String) extends Error
}
```

```scala mdoc
import cats._, cats.data._, cats.syntax.all._

object UserRepo {
  def followers(userId: Long)
    (implicit ec: ExecutionContext): EitherT[Future, Error, List[User]] =
    userId match {
      case 0L =>
        EitherT.right(Future { List(User(1, "Michael")) })
      case 1L =>
        EitherT.right(Future { List(User(0, "Vito")) })
      case x =>
        println("not found")
        EitherT.left(Future.successful { Error.UserNotFound(x) })
    }
}
import UserRepo.followers
```

Now let's try writing `isFriends0` function again.

```scala mdoc
def isFriends3(user1: Long, user2: Long)
  (implicit ec: ExecutionContext): EitherT[Future, Error, Boolean] =
  for{
    a <- followers(user1)
    b <- followers(user2)
  } yield a.exists(_.id == user2) && b.exists(_.id == user1)
```

Isn't this great? Except for the type signature and the `ExecutionContext`,
`isFriends3` is identical to `isFriends0`.

Now let's try using this.

```scala mdoc
{
  implicit val ec = scala.concurrent.ExecutionContext.global
  import scala.concurrent.Await
  import scala.concurrent.duration._

  Await.result(isFriends3(0, 1).value, 1 second)
}
```

When the first user is not found, `EitherT` will short circuit.

```scala mdoc
{
  implicit val ec = scala.concurrent.ExecutionContext.global
  import scala.concurrent.Await
  import scala.concurrent.duration._

  Await.result(isFriends3(2, 3).value, 1 second)
}
```

Note that `"not found"` is printed only once.

Unlike the `StateTReaderTOption` example, `EitherT` seems usable in many situations.

That's it for today!
