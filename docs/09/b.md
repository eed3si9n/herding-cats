---
out: safe-rpn-calculator.html
---

### Making a safe RPN calculator

LYAHFGG:

> When we were solving the problem of implementing a RPN calculator, we noted that it worked fine as long as the input that it got made sense.

We have not covered the chapter on RPN calculator,
so let's translate it into Scala.

```console:new
scala> def foldingFunction(list: List[Double], next: String): List[Double] =
         (list, next) match {
           case (x :: y :: ys, "*") => (y * x) :: ys
           case (x :: y :: ys, "+") => (y + x) :: ys
           case (x :: y :: ys, "-") => (y - x) :: ys
           case (xs, numString) => numString.toInt :: xs
         }
scala> def solveRPN(s: String): Double =
         (s.split(' ').toList.
         foldLeft(Nil: List[Double]) {foldingFunction}).head
scala> solveRPN("10 4 3 + 2 * -")
```

Looks like it's working.

The next step is to change the folding function to handle errors gracefully. We can implement `parseInt` as follows:


```console
scala> import scala.util.Try
scala> def parseInt(x: String): Option[Int] =
         (scala.util.Try(x.toInt) map { Some(_) }
         recover { case _: NumberFormatException => None }).get
scala> parseInt("1")
scala> parseInt("foo")
```

Here's the updated folding function:

```console
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> def foldingFunction(list: List[Double], next: String): Option[List[Double]] =
         (list, next) match {
           case (x :: y :: ys, "*") => ((y * x) :: ys).some
           case (x :: y :: ys, "+") => ((y + x) :: ys).some
           case (x :: y :: ys, "-") => ((y - x) :: ys).some
           case (xs, numString) => parseInt(numString) map {_ :: xs}
         }
scala> foldingFunction(List(3, 2), "*")
scala> foldingFunction(Nil, "*")
scala> foldingFunction(Nil, "wawa")
```

Finally, here's the updated `solveRPN` using `foldM`:

```console
scala> import cats._, cats.std.all._
scala> def solveRPN(s: String): Option[Double] =
         for {
           List(x) <- (Foldable[List].foldM(s.split(' ').toList, Nil: List[Double]) {foldingFunction})
         } yield x
scala> solveRPN("1 2 * 4 +")
scala> solveRPN("1 2 * 4")
scala> solveRPN("1 8 garbage")
```
