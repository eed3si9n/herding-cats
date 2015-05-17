---
out: about-laws.html
---

### About Laws

We covered lots of things around laws today. Why do we need laws anyway?

Laws are important because laws are important is a tautological statement, but there's a grain of truth to it. Like traffic laws dictating that we drive on one side within a patch of land, some laws are convenient *if everyone follows them*.

What Cats/Haskell-style function programming allows us to do is
write code that abstracts out the data, container, execution model, etc.
The abstraction will make only the assumptions stated in the laws,
thus each `A: Monoid` needs to satisfy the laws for the abstracted code to behave properly. We can call this the *utilitarian* view.

Even if we could accept the utility, why those laws? It's because it's on 
HaskellWiki or one of SPJ's papers. They might offer a starting point with an existing implementation, which we can mimic.
We can call this the *traditionalist* view. However, there is a danger of inheriting the design choices or even limitations that were made specifically for Haskell.
Functor in category theory, for instance, is a more general term than `Functor[F]`. At least `fmap` is a function that returns `F[A] => F[B]`, which is related.
By the time we get to `map` in Scala, we lose that because of type inference.

Eventually we should tie our understanding back to math.
Monoid laws correspond with the mathematical definition of monoids, and from there we can reap the benefits of the known properties of monoids.
This is relevant especially for monoid laws, because the three laws are the same as the axioms of the category, because a monoid is a special case of a category.

For the sake of learning, I think it's ok to start out with cargo cult.
We all learn language through imitation and pattern recognition.
