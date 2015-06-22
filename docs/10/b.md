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

A little known fact about Yoshida-san outside of Tokyo, is that he majored in Chinese calligraphy. Apparently he spent his collge days writing ancient seal scripts and carving seals:

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

```console:new
scala> :paste
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

```console
scala> def isFriends0(user1: Long, user2: Long): Either[Error, Boolean] =
         for {
           a <- followers(user1).right
           b <- followers(user2).right
         } yield a.exists(_.id == user2) && b.exists(_.id == user1)
```

Now suppose we want to make the database access async
so we changed the `followers` to return a `Future` like this:

```console
scala> :paste
import scala.concurrent.{ Future, ExecutionContext }
object UserRepo {
  def followers(userId: Long): Future[Either[Error, List[User]]] = ???
}
import UserRepo.followers
```

Now, how would `isFriends0` look like? Here's one way of writing this:

```console
scala> def isFriends1(user1: Long, user2: Long)
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

```console
scala> def isFriends2(user1: Long, user2: Long)
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
I don't want to imagine doing this for *every* functions that uses `Future[Either[Error, A]]`.

#### XorT datatype

Cats comes with `XorT` datatype, which is a monad transformer for `Xor` that we saw on [day 7][Xor].

```scala
/**
 * Transformer for `Xor`, allowing the effect of an arbitrary type constructor `F` to be combined with the
 * fail-fast effect of `Xor`.
 *
 * `XorT[F, A, B]` wraps a value of type `F[A Xor B]`. An `F[C]` can be lifted in to `XorT[F, A, C]` via `XorT.right`,
 * and lifted in to a `XorT[F, C, B]` via `XorT.left`.
 */
case class XorT[F[_], A, B](value: F[A Xor B]) {
  ....
}
```

Here's `UserRepo.followers` with a dummy implementation:

```console
scala> :paste
import cats._, cats.std.all._
import cats.data.XorT
object UserRepo {
  def followers(userId: Long)
    (implicit ec: ExecutionContext): XorT[Future, Error, List[User]] =
    userId match {
      case 0L =>
        XorT.right(Future { List(User(1, "Michael")) })
      case 1L =>
        XorT.right(Future { List(User(0, "Vito")) })
      case x =>
        println("not found")
        XorT.left(Future.successful { Error.UserNotFound(x) })
    }
}
import UserRepo.followers
```

Now let's try writing `isFriends0` function again.

```console
scala> def isFriends3(user1: Long, user2: Long)
         (implicit ec: ExecutionContext): XorT[Future, Error, Boolean] =
         for{
           a <- followers(user1)
           b <- followers(user2)
         } yield a.exists(_.id == user2) && b.exists(_.id == user1)
```

Isn't this great? Except for the type signature and the `ExecutionContext`,
`isFriends3` is identical to `isFriends0`.

Now let's try using this.

```console
scala> implicit val ec = scala.concurrent.ExecutionContext.global
scala> import scala.concurrent.Await
scala> import scala.concurrent.duration._
scala> Await.result(isFriends3(0, 1).value, 1 second)
```

When the first user is not found, `XorT` will short circuit.

```scala
scala> Await.result(isFriends3(2, 3).value, 1 second)
not found
res34: cats.data.Xor[Error,Boolean] = Left(UserNotFound(2))
```

Note that `"not found"` is printed only once.

Unlike the `StateTReaderTOption` example, `XorT` seems usable in many situations.

That's it for today!
