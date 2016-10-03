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

import io.strati.functional.exception.WrappedCheckedException;
import io.strati.functional.function.TryFunction;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static io.strati.functional.Try.*;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 */

public class TryTest {

  @Test
  public void testCreateAndGet() {
    assertEquals("foo", success("foo").get());
    assertEquals("foo", success("foo").getOrElse("bar"));
    assertEquals("foo", failure(new Throwable()).getOrElse("foo"));
    assertEquals("foo", failure(new Throwable()).orElse(success("foo")).get());
    assertEquals("foo", success("foo").orElse(success("bar")).get());
    assertEquals("foo", ofFailable(() -> "foo").get());
  }

  @Test(expected = NullPointerException.class)
  public void testGetFailure() {
    failure(new NullPointerException()).get();
  }

  @Test(expected = WrappedCheckedException.class)
  public void testGetFailureWrapped() {
    failure(new Throwable()).get();
  }

  @Test(expected = NullPointerException.class)
  public void testGetNestedFailure() {
    failure(failure(new NullPointerException())).get();
  }

  @Test
  public void testIfSuccess() {
    success("foo").ifSuccess(s -> assertEquals("foo", s));
    failure(new Throwable()).ifSuccess(s -> {
      throw new Exception("This shouldn't be called.");
    });
    assertTrue(success("foo").ifSuccess(s -> {
      throw new Exception();
    }).isFailure());
    assertEquals("foo", success("foo").ifSuccess(i -> {
    }).get());

    final AtomicBoolean test = new AtomicBoolean(false);

    success(13).ifSuccess(() -> test.set(true));
    assertTrue(test.get());

    failure(new Exception()).ifSuccess(() -> test.set(false));
    assertTrue(test.get());

    assertTrue(success(13).ifSuccess(() -> {
      throw new Exception();
    }).isFailure());
  }

  @Test
  public void testIfFailure() {
    failure(new Throwable("foo")).ifFailure(e -> assertEquals("foo", e.getMessage()));
    success(13).ifFailure(e -> {
      throw new Exception("This shouldn't be called.");
    }).get();
    assertEquals("foo", failure(new Throwable("foo")).ifFailure(e -> {
    }).getOrElse("foo"));

    failure(new RuntimeException())
        .ifFailure(ArrayIndexOutOfBoundsException.class, e -> {
          throw new AssertionError("Wrong ifFailure branch entered!");
        })
        .ifFailure(NoSuchElementException.class, e -> {
          throw new AssertionError("Wrong ifFailure branch entered!");
        })
        .ifFailure(NoSuchFieldException.class, e -> {
          throw new AssertionError("Wrong ifFailure branch entered!");
        })
        .ifFailure(Exception.class, e -> assertTrue(RuntimeException.class.isInstance(e)));
  }

  @Test
  public void testFlatMap() throws Throwable {
    assertEquals("foobar", success("foo").flatMap(foo -> success(foo + "bar")).get());
    assertTrue(failure(new Throwable()).flatMap(x -> success("foo")).isFailure());
    assertTrue(success("foo").flatMap(x -> failure(new Throwable())).isFailure());
    assertTrue(success("foo").flatMap(x -> {
      throw new Exception();
    }).isFailure());

    assertEquals("foo", success(13).flatMap(() -> success("foo")).get());
    assertTrue(failure(new Exception()).flatMap(() -> success("foo")).isFailure());
    assertTrue(success(13).flatMap(() -> failure(new Exception())).isFailure());
    assertTrue(success("foo").flatMap(() -> {
      throw new Exception();
    }).isFailure());

    // Monad left identity law
    TryFunction<String, Try<String>> f = s -> success(s + "bar");
    assertEquals(
        f.apply("foo").get(),
        success("foo").flatMap(f).get()
    );

    // Monad right identity law
    assertEquals(
        success("foo").flatMap(Try::success).get(),
        success("foo").get()
    );

    // Monad associativity law
    assertEquals(
        success("foo").flatMap(foo -> success(foo + "bar")).flatMap(foobar -> success(foobar + "baz")).get(),
        success("foo").flatMap(foo -> success(foo + "bar").flatMap(foobar -> success(foobar + "baz"))).get()
    );

    // Covariance (compile-time check)
    Try<TestObjectSuper> typ0 = success(new TestObject()).flatMap(o -> success(new TestObjectSub()));
    Try<TestObject> typ1 = success(new TestObject()).flatMap(o -> success(new TestObjectSub()));
    Try<TestObjectSub> typ2 = success(new TestObject()).flatMap(o -> success(new TestObjectSub()));
    Try<? extends TestObject> type3 = success(new TestObject()).flatMap(o -> success(new TestObjectSub()));
  }

  @Test
  public void testMap() {
    assertEquals("foobar", success("foo").map(foo -> foo + "bar").get());
    assertTrue(failure(new Throwable()).map(foo -> foo + "bar").isFailure());
    assertTrue(success(13).map(i -> {
      throw new Exception();
    }).isFailure());

    assertEquals("foo", success(13).map(() -> "foo").get());
    assertTrue(failure(new Exception()).map(() -> "foo").isFailure());
    assertTrue(success(13).map(() -> {
      throw new Exception();
    }).isFailure());

    // Functor law 1
    assertEquals("foo", success("foo").map(i -> i).get());

    // Functor law 2
    TryFunction<Integer, Integer> f = i -> i * 2;
    TryFunction<Integer, String> g = i -> i.toString();
    assertEquals(
        success(13).map(f).map(g).get(),
        success(13).map(i -> g.apply(f.apply(i))).get()
    );

    // Covariance (compile-time check)
    Try<TestObjectSuper> typ0 = success(new TestObject()).map(o -> new TestObjectSub());
    Try<TestObject> typ1 = success(new TestObject()).map(o -> new TestObjectSub());
    Try<TestObjectSub> typ2 = success(new TestObject()).map(o -> new TestObjectSub());
    Try<? extends TestObject> type3 = success(new TestObject()).map(o -> new TestObjectSub());
  }

  @Test
  public void testFilter() {
    assertTrue(success(13).filter(i -> true).isSuccess());
    assertTrue(success(37).filter(i -> false).isFailure());
    assertTrue(failure(new Throwable()).filter(i -> true).isFailure());
    assertTrue(success(13).filter(i -> {
      throw new Exception();
    }).isFailure());

    assertTrue(success(13).filter(i -> true, new Exception()).isSuccess());
    assertTrue(success(37).filter(i -> false, new Exception()).isFailure());
    assertTrue(failure(new Throwable()).filter(i -> true, new Exception()).isFailure());
    assertTrue(success(13).filter(i -> {
      throw new Exception();
    }, new Exception()).isFailure());

    assertTrue(success(13).filter(i -> true, i -> failure(new Exception())).isSuccess());
    assertTrue(success(37).filter(i -> false, i -> failure(new Exception())).isFailure());
    assertEquals("bar", success("foo").filter(i -> false, i -> success("bar")).get());
    assertEquals("foo", success("foo").filter(i -> false, Try::success).get());
    assertTrue(failure(new Throwable()).filter(i -> true, i -> success(13)).isFailure());
    assertTrue(success(13).filter(i -> {
      throw new Exception();
    }, i -> success(37)).isFailure());
    assertEquals("foo", success("foo").filter(i -> true, i -> {
      throw new Exception();
    }).get());
    assertTrue(success(13).filter(i -> false, i -> {
      throw new Exception();
    }).isFailure());

    assertTrue(success(13).filter(i -> true, () -> failure(new Exception())).isSuccess());
    assertTrue(success(37).filter(i -> false, () -> failure(new Exception())).isFailure());
    assertEquals("bar", success("foo").filter(i -> false, () -> success("bar")).get());
    assertTrue(failure(new Throwable()).filter(i -> true, () -> success(13)).isFailure());
    assertTrue(success(13).filter(i -> {
      throw new Exception();
    }, () -> success(37)).isFailure());
    assertEquals("foo", success("foo").filter(i -> true, () -> {
      throw new Exception();
    }).get());
    assertTrue(success(13).filter(i -> false, () -> {
      throw new Exception();
    }).isFailure());
  }

  @Test
  public void testRecoverWith() {
    assertEquals("foo", success("foo").recoverWith(e -> success("bar")).get());
    assertEquals("foo", failure(new Throwable()).recoverWith(e -> success("foo")).get());
    assertTrue(failure(new Throwable()).recoverWith(e -> failure(new Exception())).isFailure());
    assertTrue(failure(new Throwable()).recoverWith(e -> {
      throw new Exception();
    }).isFailure());

    assertTrue(failure(new Exception()).recoverWith(Exception.class, e -> failure(new Exception())).isFailure());
    assertTrue(failure(new Exception()).recoverWith(Exception.class, e -> {
      throw new Exception();
    }).isFailure());

    Try<Object> failure = Try.ofFailable(() -> failure(new Exception("message")).get());
    assertTrue(failure.isFailure());
    failure.ifFailure(e -> assertEquals("java.lang.Exception: message", e.getMessage()));

    assertEquals(
        "yep",
        failure(new RuntimeException())
            .recoverWith(ArrayIndexOutOfBoundsException.class, e -> success("nope"))
            .recoverWith(NoSuchElementException.class, e -> success("nope"))
            .recoverWith(NoSuchFieldException.class, e -> success("nope"))
            .recoverWith(Exception.class, e -> success("yep"))
            .get());

    assertTrue(failure(new RuntimeException())
        .recoverWith(ArrayIndexOutOfBoundsException.class, e -> success("nope"))
        .recoverWith(NoSuchElementException.class, e -> success("nope"))
        .recoverWith(NoSuchFieldException.class, e -> success("nope"))
        .isFailure());

    Try<Object> recoverWithFailure = failure(new RuntimeException())
        .recoverWith(ArrayIndexOutOfBoundsException.class, e -> success("nope"))
        .recoverWith(NoSuchElementException.class, e -> success("nope"))
        .recoverWith(RuntimeException.class, e -> failure(new Exception("yep")));

    recoverWithFailure
        .ifFailure(e -> assertEquals("yep", e.getMessage()))
        .ifSuccess(x -> {
          throw new AssertionError("was Success but should be Failure!");
        });
  }

  @Test
  public void testRecover() {
    assertEquals("foo", success("foo").recover(e -> "bar").get());
    assertEquals("foo", failure(new Throwable()).recover(e -> "foo").get());
    assertTrue(failure(new Exception()).recover(e -> {
      throw new Exception();
    }).isFailure());

    assertEquals("foo", failure(new Exception()).recover(Exception.class, e -> "foo").get());
    assertEquals("foo", failure(new RuntimeException()).recover(Exception.class, e -> "foo").get());
    assertTrue(failure(new Exception()).recover(RuntimeException.class, e -> "foo").isFailure());
    assertTrue(failure(new Exception()).recover(Exception.class, e -> {
      throw new Exception();
    }).isFailure());

    assertEquals(
        "yep",
        failure(new RuntimeException())
            .recover(ArrayIndexOutOfBoundsException.class, e -> "nope")
            .recover(NoSuchElementException.class, e -> "nope")
            .recover(NoSuchFieldException.class, e -> "nope")
            .recover(Exception.class, e -> "yep")
            .get());

    assertTrue(failure(new RuntimeException())
        .recover(ArrayIndexOutOfBoundsException.class, e -> "nope")
        .recover(NoSuchElementException.class, e -> "nope")
        .recover(NoSuchFieldException.class, e -> "nope")
        .isFailure());
  }

  @Test
  public void testToOptional() {
    assertTrue(success(13).toOptional().isPresent());
    assertEquals("foo", success("foo").toOptional().get());
    assertFalse(success(null).toOptional().isPresent());
    assertFalse(failure(new Throwable()).toOptional().isPresent());
  }

  @Test
  public void testApply() {
    assertEquals("foo", success(13).apply(t -> success("foo")).get());
    assertEquals("foo", failure(new Exception()).apply(t -> success("foo")).get());
    assertTrue(success(13).apply(t -> failure(new Exception())).isFailure());
    assertTrue(success(13).apply(t -> {
      throw new RuntimeException();
    }).isFailure());
    assertEquals("foobar", success(13).apply(t -> success("foo")).apply(t -> success(t.get() + "bar")).get());
    assertEquals("bar", success(13).apply(t -> success("foo")).apply(t -> failure(new Exception())).apply(t -> success("bar")).get());
    assertEquals(
        success(13).apply(t -> success("foo")).apply(t -> failure(new Exception())).apply(t -> success("bar")).get(),
        success(13).apply(s -> success("foo").apply(t -> failure(new Exception()).apply(u -> success("bar")))).get()
    );
  }

  @Test
  public void testOfFailable() {
    assertTrue(ofFailable(() -> {
      throw new RuntimeException();
    }).isFailure());
    assertTrue(ofFailable(() -> 2600).isSuccess());

    assertTrue(ofFailable(() -> {
    }).isSuccess());
    assertTrue(ofFailable(() -> {
      if (true) throw new NullPointerException();
      return;
    }).isFailure());
  }

  @Test
  public void testListCollectorSuccess() {
    List<Try<Integer>> input = new ArrayList<Try<Integer>>() {{
      add(success(1));
      add(success(2));
      add(success(3));
    }};
    Try<List<Integer>> output = input.stream().collect(Try.listCollector());
    assertTrue(output.isSuccess());
    assertEquals(3, output.get().size());
    IntStream.of(1, 2, 3).forEach(i -> assertTrue(i == output.get().get(i - 1)));
  }

  @Test
  public void testListCollectorFailure() {
    Try<List<Integer>> output1 = new ArrayList<Try<Integer>>() {{
      add(success(1));
      add(success(2));
      add(failure(new Throwable()));
      add(success(3));
      add(success(4));
    }}.stream().collect(Try.listCollector());
    assertTrue(output1.isFailure());

    Try<List<Integer>> output2 = new ArrayList<Try<Integer>>() {{
      add(failure(new Throwable()));
    }}.stream().collect(Try.listCollector());
    assertTrue(output2.isFailure());

    Try<List<Integer>> output3 = new ArrayList<Try<Integer>>() {{
      add(failure(new Throwable()));
      add(success(1));
    }}.stream().collect(Try.listCollector());
    assertTrue(output3.isFailure());

    Try<List<Integer>> output4 = new ArrayList<Try<Integer>>() {{
      add(success(1));
      add(failure(new Throwable()));
    }}.stream().collect(Try.listCollector());
    assertTrue(output4.isFailure());
  }

  @Test
  public void testListCollectorParallelSuccess() {
    List<Try<Integer>> input = new ArrayList<Try<Integer>>() {{
      add(success(1));
      add(success(2));
      add(success(3));
    }};
    Try<List<Integer>> output = input.parallelStream().collect(Try.listCollector());
    assertTrue(output.isSuccess());
    assertEquals(3, output.get().size());
    IntStream.of(1, 2, 3).forEach(i -> assertTrue(i == output.get().get(i - 1)));
  }

  @Test
  public void testListCollectorParallelFailure() {
    Try<List<Integer>> output = new ArrayList<Try<Integer>>() {{
      add(success(1));
      add(success(2));
      add(failure(new Throwable()));
      add(success(3));
      add(success(4));
    }}.parallelStream().collect(Try.listCollector());
    assertTrue(output.isFailure());
  }

  @Test
  public void testSetCollectorSuccess() {
    Set<Try<Integer>> input = new HashSet<Try<Integer>>() {{
      add(success(1));
      add(success(2));
      add(success(3));
    }};
    Try<Set<Integer>> output = input.stream().collect(Try.setCollector());
    assertTrue(output.isSuccess());
    assertEquals(3, output.get().size());

    assertTrue(output.get().containsAll(input.stream().map(Try::get).collect(toSet())));
  }

  @Test
  public void testSetCollectorFailure() {
    Try<Set<Integer>> output1 = new HashSet<Try<Integer>>() {{
      add(success(1));
      add(success(2));
      add(failure(new Throwable()));
      add(success(3));
      add(success(4));
    }}.stream().collect(Try.setCollector());
    assertTrue(output1.isFailure());

    Try<Set<Integer>> output2 = new HashSet<Try<Integer>>() {{
      add(failure(new Throwable()));
    }}.stream().collect(Try.setCollector());
    assertTrue(output2.isFailure());

    Try<Set<Integer>> output3 = new HashSet<Try<Integer>>() {{
      add(failure(new Throwable()));
      add(success(1));
    }}.stream().collect(Try.setCollector());
    assertTrue(output3.isFailure());

    Try<Set<Integer>> output4 = new HashSet<Try<Integer>>() {{
      add(success(1));
      add(failure(new Throwable()));
    }}.stream().collect(Try.setCollector());
    assertTrue(output4.isFailure());
  }

  @Test
  public void testSetCollectorParallelSuccess() {
    Set<Try<Integer>> input = new HashSet<Try<Integer>>() {{
      add(success(1));
      add(success(2));
      add(success(3));
    }};
    Try<Set<Integer>> output = input.parallelStream().collect(Try.setCollector());
    assertTrue(output.isSuccess());
    assertEquals(3, output.get().size());

    assertTrue(output.get().containsAll(input.stream().map(Try::get).collect(toSet())));
  }

  @Test
  public void testSetCollectorParallelFailure() {
    Try<Set<Integer>> output = new ArrayList<Try<Integer>>() {{
      add(success(1));
      add(success(2));
      add(failure(new Throwable()));
      add(success(3));
      add(success(4));
    }}.parallelStream().collect(Try.setCollector());
    assertTrue(output.isFailure());
  }

  private class TestObjectSuper {
  }

  private class TestObject extends TestObjectSuper {
  }

  private class TestObjectSub extends TestObject {
  }

}
