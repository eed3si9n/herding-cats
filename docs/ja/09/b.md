---
out: safe-rpn-calculator.html
---

### 安全な RPN 電卓を作ろう

LYAHFGG:

> 第10章で逆ポーランド記法 (RPN) の電卓を実装せよという問題を解いたときには、この電卓は文法的に正しい入力が与えられる限り正しく動くよ、という注意書きがありました。

最初に RPN 電卓を作った章は飛ばしたけど、コードはここにあるから Scala に訳してみる:

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

動作しているみたいだ。


次に畳み込み関数がエラーを処理できるようにする。`parseInt` は以下のように実装できる:

```scala mdoc:reset
import scala.util.Try

def parseInt(x: String): Option[Int] =
  (scala.util.Try(x.toInt) map { Some(_) }
  recover { case _: NumberFormatException => None }).get

parseInt("1")

parseInt("foo")
```

以下が更新された畳込み関数:

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

以下が `foldM` を用いて書いた `solveRPN` だ:

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
