
### Sets

Before we go abtract, we're going to look at some concrete categories.
This is actually a good thing, since we only saw one category yesterday.

The category of sets and total functions are denoted by **Sets** written in bold.
In Scala, this can be encoded roughly as types and functions between them, such as `Int => String`. There's apprently a philosophical debate on whether it is correct because programming languages admit bottom type (`Nothing`), exceptions, and non-terminating code. For the sake of convenience, I'm happy to ignore it for now and pretend we can encode **Sets**.

### Sets<sub>fin</sub>

The category of all finite sets and total functions between them are called **Sets<sub>fin</sub>**.
