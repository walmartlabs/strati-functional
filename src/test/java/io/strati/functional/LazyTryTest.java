/*
 * Copyright 2016 WalmartLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.strati.functional;

import io.strati.functional.function.TryFunction;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.strati.functional.Try.failure;
import static io.strati.functional.Try.success;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 */

public class LazyTryTest {

  @Test
  public void testIfSuccess_withConsumer() {
    AtomicInteger ai = new AtomicInteger(1);
    LazyTry<Integer> lazyTry1 = LazyTry.ofFailable(() -> 2).ifSuccess(i -> ai.set(i + 1));
    assertTrue(1 == ai.get());
    Try<Integer> res = lazyTry1.run();
    assertTrue(2 == res.get());
    assertTrue(3 == ai.get());

    LazyTry<Integer> lazyTry2 = LazyTry.ofFailable(() -> 2).ifSuccess(i -> {
      throw new Exception();
    });
    assertTrue(lazyTry2.run().isFailure());

    LazyTry<Integer> lazyTry3 = LazyTry.<Integer>ofTry(() -> failure(new Exception())).ifSuccess(i -> ai.set(1337));
    assertTrue(lazyTry3.run().isFailure());
    assertTrue(1337 != ai.get());
  }

  @Test
  public void testIfSuccess_withRunnable() {
    AtomicInteger ai = new AtomicInteger(1);
    LazyTry<Integer> lazyTry1 = LazyTry.ofFailable(() -> 2).ifSuccess(() -> ai.set(3));
    assertTrue(1 == ai.get());
    Try<Integer> res = lazyTry1.run();
    assertTrue(2 == res.get());
    assertTrue(3 == ai.get());

    LazyTry<Integer> lazyTry2 = LazyTry.ofFailable(() -> 2).ifSuccess(() -> {
      throw new Exception();
    });
    assertTrue(lazyTry2.run().isFailure());

    LazyTry<Object> lazyTry3 = LazyTry.ofTry(() -> failure(new Exception())).ifSuccess(() -> ai.set(1337));
    assertTrue(lazyTry3.run().isFailure());
    assertTrue(1337 != ai.get());
  }

  @Test
  public void testIfFailure() {
    AtomicInteger ai = new AtomicInteger(1);
    LazyTry<Object> lazyTry1 = LazyTry.ofTry(() -> failure(new Exception())).ifFailure(t -> ai.set(2));
    assertTrue(1 == ai.get());
    Try<Object> res = lazyTry1.run();
    assertTrue(res.isFailure());
    assertTrue(2 == ai.get());

    LazyTry<Object> lazyTry2 = LazyTry.ofTry(() -> failure(new Exception())).ifFailure(t -> {
      throw new Exception();
    });
    assertTrue(lazyTry2.run().isFailure());

    LazyTry<Integer> lazyTry3 = LazyTry.ofFailable(() -> 13).ifFailure(t -> ai.set(1337));
    assertTrue(lazyTry3.run().isSuccess());
    assertTrue(1337 != ai.get());
  }

  @Test
  public void testIfFailure_withSpecificException() {
    AtomicInteger ai = new AtomicInteger(1);
    LazyTry<Object> lazyTry1 = LazyTry.ofTry(() -> failure(new Exception())).ifFailure(Exception.class, t -> ai.set(2));
    assertTrue(1 == ai.get());
    Try<Object> res = lazyTry1.run();
    assertTrue(res.isFailure());
    assertTrue(2 == ai.get());

    LazyTry<Object> lazyTry2 = LazyTry.ofTry(() -> failure(new Exception())).ifFailure(Exception.class, t -> {
      throw new Exception();
    });
    assertTrue(lazyTry2.run().isFailure());

    LazyTry<Integer> lazyTry3 = LazyTry.ofFailable(() -> 13).ifFailure(Exception.class, t -> ai.set(1337));
    assertTrue(lazyTry3.run().isSuccess());
    assertTrue(1337 != ai.get());

    LazyTry.ofTry(() -> failure(new Exception())).ifFailure(RuntimeException.class, t -> ai.set(1337)).run();
    assertTrue(1337 != ai.get());
  }

  @Test
  public void testFlatMap_withFunction() throws Exception {
    AtomicInteger ai = new AtomicInteger(1);
    LazyTry<Integer> lazyTry1 = LazyTry.ofFailable(() -> 13).flatMap(i -> {
      ai.set(2600);
      return success(1337);
    });
    assertTrue(1 == ai.get());
    Try<Integer> res = lazyTry1.run();
    assertTrue(2600 == ai.get());
    assertTrue(1337 == res.get());

    assertEquals("foobar", LazyTry.ofFailable(() -> "foo").flatMap(foo -> success(foo + "bar")).run().get());

    assertTrue(LazyTry.ofFailable(() -> 13).flatMap(foo -> failure(new Exception())).run().isFailure());

    assertTrue(LazyTry.ofFailable(() -> 13).flatMap(foo -> {
      throw new Exception();
    }).run().isFailure());

    // Monad left identity law
    TryFunction<String, Try<String>> f = s -> success(s + "bar");
    LazyTry<String> lazyTryFoo = LazyTry.ofFailable(() -> "foo");
    assertEquals(
        f.apply("foo").get(),
        lazyTryFoo.flatMap(f).run().get()
    );

    // Monad right identity law
    assertEquals(
        lazyTryFoo.flatMap(Try::success).run().get(),
        lazyTryFoo.run().get()
    );

    // Monad associativity law
    assertEquals(
        lazyTryFoo.flatMap(foo -> success(foo + "bar")).flatMap(foobar -> success(foobar + "baz")).run().get(),
        lazyTryFoo.flatMap(foo -> success(foo + "bar").flatMap(foobar -> success(foobar + "baz"))).run().get()
    );
  }

  @Test
  public void testFlatMap_withSupplier() throws Exception {
    AtomicInteger ai = new AtomicInteger(1);
    LazyTry<Integer> lazyTry1 = LazyTry.ofFailable(() -> 13).flatMap(() -> {
      ai.set(2600);
      return success(1337);
    });
    assertTrue(1 == ai.get());
    Try<Integer> res = lazyTry1.run();
    assertTrue(2600 == ai.get());
    assertTrue(1337 == res.get());

    assertEquals("bar", LazyTry.ofFailable(() -> "foo").flatMap(() -> success("bar")).run().get());

    assertTrue(LazyTry.ofFailable(() -> 13).flatMap(() -> failure(new Exception())).run().isFailure());

    assertTrue(LazyTry.ofFailable(() -> 13).flatMap(() -> {
      throw new Exception();
    }).run().isFailure());
  }

  @Test
  public void testMap_withFunction() throws Exception {
    AtomicInteger ai = new AtomicInteger(1);
    LazyTry<Integer> lazyTry1 = LazyTry.ofFailable(() -> 13).map(i -> {
      ai.set(2600);
      return 1337;
    });
    assertTrue(1 == ai.get());
    Try<Integer> res = lazyTry1.run();
    assertTrue(2600 == ai.get());
    assertTrue(1337 == res.get());

    assertEquals("foobar", LazyTry.ofFailable(() -> "foo").map(foo -> foo + "bar").run().get());

    assertTrue(LazyTry.ofFailable(() -> 13).map(i -> failure(new Exception())).run().isSuccess());

    assertTrue(LazyTry.ofFailable(() -> 13).map(i -> {
      throw new Exception();
    }).run().isFailure());

    // Functor law 1
    assertEquals("foo", LazyTry.ofFailable(() -> "foo").map(i -> i).run().get());

    // Functor law 2
    TryFunction<Integer, Integer> f = i -> i * 2;
    TryFunction<Integer, String> g = i -> i.toString();
    LazyTry<Integer> lazyTry = LazyTry.ofFailable(() -> 13);
    assertEquals(
        lazyTry.map(f).map(g).run().get(),
        lazyTry.map(i -> g.apply(f.apply(i))).run().get()
    );
  }

  @Test
  public void testMap_withSupplier() throws Exception {
    AtomicInteger ai = new AtomicInteger(1);
    LazyTry<Integer> lazyTry1 = LazyTry.ofFailable(() -> 13).map(() -> {
      ai.set(2600);
      return 1337;
    });
    assertTrue(1 == ai.get());
    Try<Integer> res = lazyTry1.run();
    assertTrue(2600 == ai.get());
    assertTrue(1337 == res.get());

    assertEquals("bar", LazyTry.ofFailable(() -> "foo").map(() -> "bar").run().get());

    assertTrue(LazyTry.ofFailable(() -> 13).map(() -> failure(new Exception())).run().isSuccess());

    assertTrue(LazyTry.ofFailable(() -> 13).map(() -> {
      throw new Exception();
    }).run().isFailure());
  }

  @Test
  public void testFilter() {
    LazyTry<Integer> lazyTryInt = LazyTry.success(13);
    LazyTry<String> lazyTryString = LazyTry.ofFailable(() -> "foo");
    LazyTry<Integer> failingLazyTry = LazyTry.failure( new Exception());

    assertTrue(lazyTryInt.filter(i -> true).run().isSuccess());
    assertEquals("foo", lazyTryString.filter(i -> true).run().get());
    assertTrue(lazyTryInt.filter(i -> false).run().isFailure());
    assertTrue(failingLazyTry.filter(i -> true).run().isFailure());
    assertTrue(lazyTryInt.filter(i -> {
      throw new Exception();
    }).run().isFailure());

    assertTrue(lazyTryInt.filter(i -> true, new Exception()).run().isSuccess());
    assertTrue(lazyTryInt.filter(i -> false, new Exception()).run().isFailure());
    assertTrue(failingLazyTry.filter(i -> true, new Exception()).run().isFailure());
    assertTrue(lazyTryInt.filter(i -> {
      throw new Exception();
    }, new Exception()).run().isFailure());

    assertTrue(lazyTryInt.filter(i -> true, i -> failure(new Exception())).run().isSuccess());
    assertTrue(lazyTryInt.filter(i -> false, i -> failure(new Exception())).run().isFailure());
    assertEquals("bar", lazyTryString.filter(i -> false, i -> success("bar")).run().get());
    assertEquals("foo", lazyTryString.filter(i -> false, Try::success).run().get());
    assertTrue(failingLazyTry.filter(i -> true, i -> success(13)).run().isFailure());
    assertTrue(lazyTryInt.filter(i -> {
      throw new Exception();
    }, i -> success(37)).run().isFailure());
    assertEquals("foo", lazyTryString.filter(i -> true, i -> {
      throw new Exception();
    }).run().get());
    assertTrue(lazyTryInt.filter(i -> false, i -> {
      throw new Exception();
    }).run().isFailure());

    assertTrue(lazyTryInt.filter(i -> true, () -> failure(new Exception())).run().isSuccess());
    assertTrue(lazyTryInt.filter(i -> false, () -> failure(new Exception())).run().isFailure());
    assertEquals("bar", lazyTryString.filter(i -> false, () -> success("bar")).run().get());
    assertTrue(failingLazyTry.filter(i -> true, () -> success(13)).run().isFailure());
    assertTrue(lazyTryInt.filter(i -> {
      throw new Exception();
    }, () -> success(37)).run().isFailure());
    assertEquals("foo", lazyTryString.filter(i -> true, () -> {
      throw new Exception();
    }).run().get());
    assertTrue(lazyTryInt.filter(i -> false, () -> {
      throw new Exception();
    }).run().isFailure());
  }

  @Test
  public void testRecoverWith() {
    LazyTry<String> lazyTryString = LazyTry.ofFailable(() -> "foo");
    LazyTry<String> failingLazyTry = LazyTry.ofFailable(() -> {
      throw new Exception();
    });

    assertEquals("foo", lazyTryString.recoverWith(e -> success("bar")).run().get());
    assertEquals("foo", failingLazyTry.recoverWith(e -> success("foo")).run().get());
    assertTrue(failingLazyTry.recoverWith(e -> failure(new Exception())).run().isFailure());
    assertTrue(failingLazyTry.recoverWith(e -> {
      throw new Exception();
    }).run().isFailure());

    assertTrue(failingLazyTry.recoverWith(Exception.class, e -> failure(new Exception())).run().isFailure());
    assertTrue(failingLazyTry.recoverWith(Exception.class, e -> {
      throw new Exception();
    }).run().isFailure());

    Try<Object> failure = LazyTry.ofFailable(() -> failure(new Exception("message")).get()).run();
    assertTrue(failure.isFailure());
    failure.ifFailure(e -> assertEquals("java.lang.Exception: message", e.getMessage()));
  }

  @Test
  public void testRecover() {
    LazyTry<String> lazyTryString = LazyTry.ofFailable(() -> "foo");
    LazyTry<String> failingLazyTry = LazyTry.ofFailable(() -> {
      throw new Exception();
    });

    assertEquals("foo", lazyTryString.recover(e -> "bar").run().get());
    assertEquals("foo", failingLazyTry.recover(e -> "foo").run().get());
    assertTrue(failingLazyTry.recover(e -> {
      throw new Exception();
    }).run().isFailure());

    assertEquals("foo", failingLazyTry.recover(Exception.class, e -> "foo").run().get());
    assertEquals("foo", failingLazyTry.recover(Exception.class, e -> "foo").run().get());
    assertTrue(failingLazyTry.recover(RuntimeException.class, e -> "foo").run().isFailure());
    assertTrue(failingLazyTry.recover(Exception.class, e -> {
      throw new Exception();
    }).run().isFailure());
  }

  @Test
  public void testOfFailable() {
    assertTrue(LazyTry.ofFailable(() -> {
      throw new RuntimeException();
    }).run().isFailure());
    assertTrue(LazyTry.ofFailable(() -> 2600).run().isSuccess());

    assertTrue(LazyTry.ofFailable(() -> {
    }).run().isSuccess());
    assertTrue(LazyTry.ofFailable(() -> {
      if (true) throw new NullPointerException();
      return;
    }).run().isFailure());
  }

  @Test
  public void testApply() {
    assertEquals("foo", LazyTry.ofFailable(() -> "foo").apply(LazyTry::run).get());
  }

  @Test
  public void testCreateAsLambda() {
    LazyTry<String> foo = () -> Try.ofFailable(() -> "foo");
    assertEquals("foo", foo.run().get());

    LazyTry<String> boom = () -> Try.ofFailable(() -> {
      throw new RuntimeException("boom");
    });

    assertTrue(boom.run().isFailure());
  }

}
