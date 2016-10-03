package io.strati.functional.resilience;

import io.strati.functional.Try;
import io.strati.functional.exception.CircuitBreakerOpenException;
import io.strati.functional.exception.WrappedCheckedException;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 */

public class CircuitBreakerTest {

  @Test
  public void testCBWithRunnable_OpenCircuitThenSucceed() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker(2, 2000);
    assertTrue(cb.isClosed());

    Try<Void> result1 = cb.attempt(() -> System.out.println(3 / 0));
    assertFailure(ArithmeticException.class, result1);
    assertTrue(cb.isClosed());

    Try<Void> result2 = cb.attempt(() -> System.out.println(3 / 0));
    assertFailure(ArithmeticException.class, result2);
    assertTrue(cb.isOpen());

    Thread.sleep(1000);
    assertTrue(cb.isOpen());

    Thread.sleep(1000);
    assertTrue(cb.isHalfOpen());

    Try<Void> result3 = cb.attempt(() -> System.out.println("foo"));
    assertTrue("call should have succeeded", result3.isSuccess());
    assertTrue(cb.isClosed());
  }

  @Test
  public void testCBWithRunnable_OpenCircuitThenFail() throws InterruptedException {
    Duration timeout = Duration.ofMillis(2000);
    CircuitBreaker cb = new CircuitBreaker(2, timeout);
    assertTrue(cb.isClosed());

    Try<Void> result1 = cb.attempt(() -> System.out.println(3 / 0));
    assertFailure(ArithmeticException.class, result1);
    assertTrue(cb.isClosed());

    Try<Void> result2 = cb.attempt(() -> System.out.println(3 / 0));
    assertFailure(ArithmeticException.class, result2);
    assertTrue(cb.isOpen());

    Thread.sleep(1000);
    assertTrue(cb.isOpen());

    Thread.sleep(1000);
    assertTrue(cb.isHalfOpen());

    Try<Void> result3 = cb.attempt(() -> System.out.println(3 / 0));
    assertFailure(ArithmeticException.class, result3);
    assertTrue(cb.isOpen());
  }

  @Test
  public void testCBWithRunnable_closedCiruit() {
    CircuitBreaker cb = new CircuitBreaker(2, 2000);
    assertTrue(cb.isClosed());
    IntStream.range(0, 100)
        .forEach(i -> {
          cb.attempt(() -> System.out.println("> " + i));
          assertTrue(cb.isClosed());
        });
  }

  @Test
  public void testCBWithRunnable_openCiruit() {
    CircuitBreaker cb = new CircuitBreaker(1, 1337);
    assertTrue(cb.isClosed());
    IntStream.range(0, 100)
        .forEach(i -> {
          Try<Void> result = cb.attempt(() -> System.out.println(3 / 0));
          assertTrue("call should have failed", result.isFailure());
          assertTrue("circuit breaker should be open", cb.isOpen());
        });
  }

  @Test
  public void testCBWithSupplier_OpenCircuitThenSucceed() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker(2, 2000);
    assertTrue(cb.isClosed());

    Try<Integer> result1 = cb.attempt(() -> 3/0);
    assertFailure(ArithmeticException.class, result1);
    assertTrue(cb.isClosed());

    Try<Integer> result2 = cb.attempt(() -> 3 / 0);
    assertFailure(ArithmeticException.class, result2);
    assertTrue(cb.isOpen());

    Thread.sleep(1000);
    assertTrue(cb.isOpen());

    Thread.sleep(1000);
    assertTrue(cb.isHalfOpen());

    Try<Integer> result3 = cb.attempt(() -> 3/1);
    assertTrue("call should have succeeded", result3.isSuccess());
    assertTrue("result should be 3", 3 == result3.get());
    assertTrue(cb.isClosed());
  }

  @Test
  public void testCBWithSupplier_OpenCircuitThenFail() throws InterruptedException {
    Duration timeout = Duration.ofMillis(2000);
    CircuitBreaker cb = new CircuitBreaker(2, timeout);
    assertTrue(cb.isClosed());

    Try<Integer> result1 = cb.attempt(() -> 3 / 0);
    assertFailure(ArithmeticException.class, result1);
    assertTrue(cb.isClosed());

    Try<Integer> result2 = cb.attempt(() -> 3 / 0);
    assertFailure(ArithmeticException.class, result2);
    assertTrue(cb.isOpen());

    Thread.sleep(1000);
    assertTrue(cb.isOpen());

    Thread.sleep(1000);
    assertTrue(cb.isHalfOpen());

    Try<Integer> result3 = cb.attempt(() -> 3 / 0);
    assertFailure(ArithmeticException.class, result3);
    assertTrue(cb.isOpen());
  }

  @Test
  public void testCB_noStatusChecks() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker(2, 1000);
    cb.attempt(() -> 3 / 0);
    cb.attempt(() -> 3 / 0);
    assertFailure(CircuitBreakerOpenException.class, cb.attempt(() -> 3 / 0));
    Thread.sleep(1000);
    assertEquals("Circuit breaker should be closed and action should succeed", "foo", cb.attempt(() -> "foo").get());
  }

  @Test
  public void testCB_OpenStateFallback() {
    CircuitBreaker cb = new CircuitBreaker(2, 1000);
    AtomicBoolean flag = new AtomicBoolean(false);
    cb.attempt(() -> 3 / 0);
    cb.attempt(() -> 3 / 0);
    assertTrue(cb.isOpen());
    assertFalse("flag should've been set to false", flag.get());

    cb.attempt(() -> 3 / 0).ifFailure(e -> flag.set(true));
    assertTrue(cb.isOpen());
    assertTrue("flag should've been set to true", flag.get());
  }

  @Test
  public void testCB_HalfOpenStateFallback() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker(2, 1000);
    AtomicBoolean flag = new AtomicBoolean(false);
    cb.attempt(() -> 3 / 0);
    cb.attempt(() -> 3 / 0);
    assertTrue(cb.isOpen());
    Thread.sleep(2000);
    assertTrue(cb.isHalfOpen());
    assertFalse("flag should've been set to false", flag.get());

    cb.attempt(() -> 3 / 0).ifFailure(e -> flag.set(true));
    assertTrue(cb.isOpen());
    assertTrue("flag should've been set to true", flag.get());
  }

  @Test
  public void testCB_getters() {
    String name = "foobar";
    int threshold = 13;
    long timeout = 1337;

    CircuitBreaker cb1 = new CircuitBreaker(name, threshold, timeout);
    cb1.getName();
    assertEquals("name should be equal", name, cb1.getName());
    assertEquals("threshold should be equal", threshold, cb1.getThreshold());
    assertEquals("timeout should be equal", timeout, cb1.getTimeout());

    CircuitBreaker cb2 = new CircuitBreaker(name, threshold, Duration.ofMillis(timeout));
    cb2.getName();
    assertEquals("name should be equal", name, cb2.getName());
    assertEquals("threshold should be equal", threshold, cb2.getThreshold());
    assertEquals("timeout should be equal", timeout, cb2.getTimeout());
  }

  private static <T> void assertFailure(Class<? extends Throwable> ex, Try<T> value) {
    assertTrue("Expected failure didn't occur: " + ex, value.isFailure());
    try {
      value.get();
    } catch (Throwable t) {
      Class<? extends Throwable> exceptionClass =
          WrappedCheckedException.class.equals(t.getClass()) ? t.getCause().getClass() : t.getClass();
      assertEquals("Unexpected exception occurred", ex, exceptionClass);
    }
  }

}
