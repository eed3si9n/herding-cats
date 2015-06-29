package example;

import java.util.Collection;

public class Loop {
  public static <E> int loop(Collection<E> coll) {
    int n = 0;
    for (E elem: coll) {
      n = n + 1;
      doSomething(elem);
    }
    return n;
  }

  public static <E> void doSomething(E e) {}
}
