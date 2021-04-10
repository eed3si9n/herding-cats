---
out: safe-rpn-calculator.html
---

### Making a safe RPN calculator

LYAHFGG:

> When we were solving the problem of implementing a RPN calculator, we noted that it worked fine as long as the input that it got made sense.

We have not covered the chapter on RPN calculator,
so let's translate it into Scala.

```scala mdoc
def foldingFunction(list: List[Double], next: String): List[Double] =
  (list, next) match {
    case (x :: y :: ys, "*") => (y * x) :: ys
    case (x :: y :: ys, "+") => (y + x) :: ys
    case (x :: y :: ys, "-") => (y - x) :: ys
    case (xs, numString) => numString.toInt :: xs
  }

def solveRPN(s: String): Double =
  (s.split(' ').toList
    .foldLeft(Nil: List[Double]) {foldingFunction}).head

solveRPN("10 4 3 + 2 * -")
```

Looks like it's working.

The next step is to change the folding function to handle errors gracefully. We can implement `parseInt` as follows:

```scala mdoc:reset
import scala.util.Try

def parseInt(x: String): Option[Int] =
  (scala.util.Try(x.toInt) map { Some(_) }
  recover { case _: NumberFormatException => None }).get

parseInt("1")

parseInt("foo")
```

Here's the updated folding function:

```scala mdoc
import cats._, cats.syntax.all._

def foldingFunction(list: List[Double], next: String): Option[List[Double]] =
  (list, next) match {
    case (x :: y :: ys, "*") => ((y * x) :: ys).some
    case (x :: y :: ys, "+") => ((y + x) :: ys).some
    case (x :: y :: ys, "-") => ((y - x) :: ys).some
    case (xs, numString) => parseInt(numString) map {_ :: xs}
  }

foldingFunction(List(3, 2), "*")

foldingFunction(Nil, "*")

foldingFunction(Nil, "wawa")
```

Finally, here's the updated `solveRPN` using `foldM`:

```scala mdoc
def solveRPN(s: String): Option[Double] =
  for {
    List(x) <- (Foldable[List].foldM(s.split(' ').toList,
                  Nil: List[Double]) {foldingFunction})
  } yield x

solveRPN("1 2 * 4 +")

solveRPN("1 2 * 4")

solveRPN("1 8 garbage")
```
