
### `Read`

LYAHFGG:

> `Read` is sort of the opposite typeclass of `Show`. The `read` function takes a string and returns a type which is a member of `Read`.

I could not find Cats' equivalent for this typeclass.

I find myself defining `Read` and its variant `ReadJs` time and time again.
Stringly typed programming is ugly.
At the same time, String is a robust data format to cross platform boundaries (e.g. JSON).
Also we humans know how to deal with them directly (e.g. command line options),
so it's hard to get away from String parsing.
If we're going to do it anyway, having `Read` makes it easier.
