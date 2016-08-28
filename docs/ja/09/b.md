---
out: safe-rpn-calculator.html
---

### 安全な RPN 電卓を作ろう

LYAHFGG:

> 第10章で逆ポーランド記法 (RPN) の電卓を実装せよという問題を解いたときには、この電卓は文法的に正しい入力が与えられる限り正しく動くよ、という注意書きがありました。

最初に RPN 電卓を作った章は飛ばしたけど、コードはここにあるから Scala に訳してみる:

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

動作しているみたいだ。


次に畳み込み関数がエラーを処理できるようにする。`parseInt` は以下のように実装できる:

```console
scala> import scala.util.Try
scala> def parseInt(x: String): Option[Int] =
         (scala.util.Try(x.toInt) map { Some(_) }
         recover { case _: NumberFormatException => None }).get
scala> parseInt("1")
scala> parseInt("foo")
```

以下が更新された畳込み関数:

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

以下が `foldM` を用いて書いた `solveRPN` だ:

```console
scala> import cats._, cats.instances.all._
scala> def solveRPN(s: String): Option[Double] =
         for {
           List(x) <- (Foldable[List].foldM(s.split(' ').toList, Nil: List[Double]) {foldingFunction})
         } yield x
scala> solveRPN("1 2 * 4 +")
scala> solveRPN("1 2 * 4")
scala> solveRPN("1 8 garbage")
```
