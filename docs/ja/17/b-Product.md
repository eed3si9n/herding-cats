---
out: Product.html
---

  [product]: http://www.scala-lang.org/api/2.12.4/scala/Product.html

### 積

> まずは集合の積を考える。集合 A と B があるとき、A と B のデカルト積は順序対 (ordered pairs) の集合となる<br>
> A × B = {(a, b)| a ∈ A, b ∈ B}

2つの座標射影 (coordinate projection) があって:<br>
![coordinate projections](../files/day17-coordinate-projections.png)

これは以下の条件を満たす:

- *fst ∘ (a, b) = a*
- *snd ∘ (a, b) = b*

この積という考えは case class やタプルの基底 trait である [scala.Product][product] にも関連する。

任意の要素 *c ∈ A × B* に対して、*c = (fst ∘ c, snd ∘ c)* ということができる。

15日目に出てきたが、明示的に単集合 **1** を導入することで要素という概念を一般化できる。

![product of sets](../files/day17-product-of-sets.png)

これをすこしきれいに直すと、積の圏論的な定義を得ることができる:

> **定義 2.15.** 任意の圏 **C** において、対象 A と B の積の図式は対象 P と射 p<sub>1</sub> と p<sub>2</sub> から構成され<br>
> ![product diagram](../files/day17-product-diagram.png)<br>
> 以下の UMP を満たす:
>
> この形となる任意の図式があるとき<br>
> ![product definition](../files/day17-product-definition.png)<br>
> 次の図式<br>
> ![product of objects](../files/day17-product-of-objects.png)<br>
> が可換となる (つまり、x<sub>1</sub> = p<sub>1</sub> u かつ x<sub>2</sub> = p<sub>2</sub> u が成立する) 一意の射 u: X => P が存在する。

「一意の射」と出てきたら UMP だなと見当がつく。

#### 積の一意性

**Sets** に立ち返ると、型*A* と型*B* があるとき、`(A, B)` を返す一意の関数があると言っているだけだ。しかし、これが全ての圏に当てはまるかどう証明すればいいだろう? 使って良いのはアルファベットと矢印だけだ。

> **命題 2.17** 積は同型を除いて一意である。

*P* と *Q* が対象 *A* と *B* の積であるとする。

![uniqueness of products](../files/day17-uniqueness-of-products.png)

1. *P* は積であるため、*p<sub>1</sub> = q<sub>1</sub> ∘ i* かつ *p<sub>2</sub> = q<sub>2</sub> ∘ i* を満たす一意の *i: P => Q* が存在する。
2. *Q* は積であるため、*q<sub>1</sub> = p<sub>1</sub> ∘ j* かつ *q<sub>2</sub> = p<sub>2</sub> ∘ j* を満たす一意の *j: Q => P* が存在する。
3. *i* と *j* を合成することで *1<sub>P</sub> = j ∘ i* が得られる。
4. 同様にして *1<sub>Q</sub> = i ∘ j*。
5. *i* は同型射、*P ≅ Q* である。∎

全ての積は同型であるため、一つを取って *A × B* と表記する。また、射 *u: X => A × B* は *⟨x<sub>1</sub>, x<sub>2</sub>⟩* と表記する。
