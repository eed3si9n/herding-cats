
### Show

LYAHFGG:

> ある値は、その値が `Show` 型クラスのインスタンスになっていれば、文字列として表現できます。

Cats で `Show` に対応する型クラスは `Show` だ:

```console:new
scala> import cats._, cats.std.all._, cats.syntax.show._
scala> 3.show
scala> "hello".show
```

Scala には既に `Any` に `toString` があるため、`Show`
を定義するのは馬鹿げているように一見見えるかもしれない。
`Any` ということは逆に何でも該当してしまうので、型安全性を失うことになる。
`toString` は何らかの親クラスが書いたゴミかもしれない:

```console:error
scala> (new {}).toString
scala> (new {}).show
```
