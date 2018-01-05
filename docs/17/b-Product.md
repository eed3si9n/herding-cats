---
out: Product.html
---

  [product]: http://www.scala-lang.org/api/2.12.4/scala/Product.html

### Product

> Let us begin by considering products of sets. Given sets A and B, the cartesian product of A and B is the set of ordered pairs<br>
> A × B = {(a, b)| a ∈ A, b ∈ B}

There are two coordinate projections:<br>
![coordinate projections](files/day17-coordinate-projections.png)

with:

- *fst ∘ (a, b) = a*
- *snd ∘ (a, b) = b*

This notion of product relates to [scala.Product][product], which is the base trait for all tuples and case classes.

For any element in *c ∈ A × B*, we have *c = (fst ∘ c, snd ∘ c)*

If we recall from day 15, we can generalize the concept of elements by introducing the singleton **1** explicitly.

![product of sets](files/day17-product-of-sets.png)


If we clean it up a bit more, we get the following categorical definition of a product:

> **Definition 2.15.** In any category **C**, a product diagram for the objects A and B consists of an object P and arrows p<sub>1</sub> and p<sub>2</sub><br>
> ![product diagram](files/day17-product-diagram.png)<br>
> satisfying the following UMP:
>
> Given any diagram of the form<br>
> ![product definition](files/day17-product-definition.png)<br>
> there exists a unique u: X => P, making the diagram<br>
> ![product of objects](files/day17-product-of-objects.png)<br>
> commute, that is, such that x<sub>1</sub> = p<sub>1</sub> u and x<sub>2</sub> = p<sub>2</sub> u.

"There exists unique u" is a giveaway that this definition is an UMP.

#### Uniqueness of products

If we step back to **Sets**, all it's saying is that given type *A* and type *B*, there's a unique function that can return `(A, B)`. But how can we prove that for all categories? All we have at our disposal are alphabets and arrows.

> **Proposition 2.17** Products are unique up to isomorphism.

Suppose we have *P* and *Q* that are products of objects *A* and *B*.

![uniqueness of products](files/day17-uniqueness-of-products.png)

1. Because *P* is a product, there is a unique *i: P => Q* such that *p<sub>1</sub> = q<sub>1</sub> ∘ i* and *p<sub>2</sub> = q<sub>2</sub> ∘ i*
2. Because *Q* is a product, there is a unique *j: Q => P* such that *q<sub>1</sub> = p<sub>1</sub> ∘ j* and *q<sub>2</sub> = p<sub>2</sub> ∘ j*
3. By composing *j* and *i* we get *1<sub>P</sub> = j ∘ i*
4. Similarly, we can also get *1<sub>Q</sub> = i ∘ j*
5. Thus *i* is an isomorphism, and *P ≅ Q* ∎

Since all products are isometric, we can just denote one as *A × B*, and the arrow *u: X => A × B* is denoted as *⟨x<sub>1</sub>, x<sub>2</sub>⟩*.
