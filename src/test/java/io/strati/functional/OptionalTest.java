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

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static io.strati.functional.Optional.*;
import static org.junit.Assert.*;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 */

public class OptionalTest {

  @Test
  public void testCreateAndGet() {
    assertEquals("foo", of("foo").get());
    assertEquals("foo", ofNullable("foo").get());
  }

  @Test(expected = NullPointerException.class)
  public void testNullAsValue() {
    of(null);
  }

  @Test(expected = NoSuchElementException.class)
  public void testGetOnEmpty() {
    empty().get();
  }

  @Test
  public void testIsPresent() {
    assertTrue(of("foo").isPresent());
    assertTrue(ofNullable("foo").isPresent());
    assertFalse(empty().isPresent());
    assertFalse(ofNullable(null).isPresent());
  }

  @Test
  public void testIsEmpty() {
    assertTrue(empty().isEmpty());
    assertTrue(ofNullable(null).isEmpty());
    assertFalse(of("foo").isEmpty());
    assertFalse(ofNullable("foo").isEmpty());
  }

  @Test
  public void testIfPresent() {
    final AtomicBoolean test = new AtomicBoolean(false);
    ofNullable("foo").ifPresent(s -> test.set(true));
    assertTrue(test.get());

    empty().ifPresent(o -> test.set(false));
    assertTrue(test.get());

    ofNullable("foo").ifPresent(s -> assertEquals("foo", s));
  }

  @Test
  public void testIfEmpty() {
    final AtomicBoolean test = new AtomicBoolean(false);
    empty().ifEmpty(() -> test.set(true));
    assertTrue(test.get());

    of("foo").ifEmpty(() -> test.set(false));
    assertTrue(test.get());

    ofNullable("foo").ifEmpty(() -> test.set(false));
    assertTrue(test.get());
  }

  @Test
  public void testFilter() {
    assertTrue(ofNullable("foo").filter(s -> s.equals("foo")).isPresent());
    assertEquals("foo", ofNullable("foo").filter(s -> s.equals("foo")).get());

    assertTrue(ofNullable("foo").filter(s -> s.equals("bar")).isEmpty());

    assertTrue(empty().filter(s -> true).isEmpty());
    assertFalse(empty().filter(s -> true).isPresent());
  }

  @Test
  public void testMap() {
    assertEquals("foobar", of("foo").map(foo -> foo + "bar").get());
    assertTrue(empty().map(s -> "foo").isEmpty());

    // Functor law 1
    assertEquals("foo", ofNullable("foo").map(s -> s).get());

    // Functor law 2
    final Function<Integer, Integer> f = i -> i * 2;
    final Function<Integer, String> g = i -> i.toString();
    assertEquals(
        ofNullable(13).map(f).map(g).get(),
        ofNullable(13).map(i -> g.apply(f.apply(i))).get()
    );

    // Covariance (compile-time check)
    Optional<TestObjectSuper> type0 = ofNullable(new TestObject()).map(o -> new TestObjectSub());
    Optional<TestObject> type1 = ofNullable(new TestObject()).map(o -> new TestObjectSub());
    Optional<TestObjectSub> type2 = ofNullable(new TestObject()).map(o -> new TestObjectSub());
    Optional<? extends TestObject> type3 = ofNullable(new TestObject()).map(o -> new TestObjectSub());
  }

  @Test
  public void testFlatMap() {
    assertEquals("foobar", ofNullable("foo").flatMap(foo -> of(foo + "bar")).get());
    assertTrue(empty().flatMap(x -> of("foo")).isEmpty());
    assertTrue(ofNullable("foo").flatMap(s -> empty()).isEmpty());
    assertTrue(ofNullable("foo").flatMap(s -> null).isEmpty());

    // Monad left identity law
    Function<String, Optional<String>> f = s -> ofNullable(s + "bar");
    assertEquals(
        f.apply("foo").get(),
        ofNullable("foo").flatMap(f).get()
    );

    // Monad right identity law
    assertEquals(
        ofNullable("foo").flatMap(Optional::ofNullable).get(),
        ofNullable("foo").get()
    );

    // Monad associativity law
    assertEquals(
        ofNullable("foo").flatMap(foo -> ofNullable(foo + "bar")).flatMap(foobar -> ofNullable(foobar + "baz")).get(),
        ofNullable("foo").flatMap(foo -> ofNullable(foo + "bar").flatMap(foobar -> ofNullable(foobar + "baz"))).get()
    );

    // Covariance (compile-time check)
    Optional<TestObjectSuper> type0 = ofNullable(new TestObject()).flatMap(o -> of(new TestObjectSub()));
    Optional<TestObject> type1 = ofNullable(new TestObject()).flatMap(o -> of(new TestObjectSub()));
    Optional<TestObjectSub> type2 = ofNullable(new TestObject()).flatMap(o -> of(new TestObjectSub()));
    Optional<? extends TestObject> type3 = ofNullable(new TestObject()).flatMap(o -> of(new TestObjectSub()));
  }

  @Test
  public void testOrElseMap() {
    assertEquals("foo", empty().orElseMap(() -> "foo").get());
    assertEquals("foo", of("foo").orElseMap(() -> "bar").get());
    assertEquals("foo", empty().map(s -> "bar").orElseMap(() -> "foo").get());
  }

  @Test
  public void testOrElseFlatMap() {
    assertEquals("foo", empty().orElseFlatMap(() -> of("foo")).get());
    assertEquals("foo", of("foo").orElseFlatMap(() -> of("bar")).get());
    assertTrue(empty().orElseFlatMap(Optional::empty).isEmpty());
    assertEquals("foo", empty().flatMap(s -> of("bar")).orElseFlatMap(() -> of("foo")).get());
  }

  @Test
  public void testOrElse() {
    assertEquals("foo", empty().orElse("foo"));
    assertEquals("foo", ofNullable("foo").orElse("bar"));
  }

  @Test
  public void testOrElseGet() {
    assertEquals("foo", empty().orElseGet(() -> "foo"));
    assertEquals("foo", ofNullable("foo").orElseGet(() -> {
      assertTrue(false);
      return "bar";
    }));
  }

  @Test(expected = RuntimeException.class)
  public void testOrElseThrow() {
    assertEquals("foo", of("foo").orElseThrow(AssertionError::new));
    empty().orElseThrow(RuntimeException::new);
  }

  @Test
  public void testStream() {
    assertEquals(0, empty().stream().count());
    assertEquals(1, of("foo").stream().count());
    assertEquals("foo", of("foo").stream().findFirst().get());
  }

  @Test(expected = RuntimeException.class)
  public void testToTry() {
    assertTrue(of("foo").toTry().isSuccess());
    assertTrue(empty().toTry().isFailure());
    assertEquals("foo", of("foo").toTry().get());
    empty().toTry().get();
  }

  @Test
  public void testToJdkOptional() {
    assertFalse(empty().toJdkOptional().isPresent());
    assertTrue(of("foo").toJdkOptional().isPresent());
    assertEquals("foo", of("foo").toJdkOptional().get());
  }

  @Test
  public void testFrom() {
    assertTrue(from(java.util.Optional.of("foo")).isPresent());
    assertTrue(from(java.util.Optional.empty()).isEmpty());
    assertEquals("foo", from(java.util.Optional.of("foo")).get());
  }

  private class TestObjectSuper {
  }

  private class TestObject extends TestObjectSuper {
  }

  private class TestObjectSub extends TestObject {
  }
}
