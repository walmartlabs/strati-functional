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

package io.strati.functional.resilience;

import io.strati.functional.LazyTry;
import io.strati.functional.Optional;
import io.strati.functional.Try;
import io.strati.functional.exception.CircuitBreakerOpenException;
import io.strati.functional.function.TryRunnable;
import io.strati.functional.function.TrySupplier;

import java.util.function.Consumer;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 *
 * Implementation of the Circuit Breaker pattern, proposed by Michael T. Nygard in his book "Release It!".
 *
 */
public class CircuitBreaker {

  private enum State {
    CLOSED, HALF_OPEN, OPEN
  }

  private String name;

  private final Object monitor = new Object();

  private State state;

  private int failures = 0;
  private final int threshold;
  private final long timeout;
  private Throwable lastObservedFailure;
  private long openTime;

  private Consumer<CircuitBreaker> toClosedStateListener;
  private Consumer<CircuitBreaker> toHalfOpenStateListener;
  private Consumer<CircuitBreaker> toOpenStateListener;

  CircuitBreaker(final String name, final int threshold, final long timeout,
                 final Consumer<CircuitBreaker> toClosedStateListener,
                 final Consumer<CircuitBreaker> toHalfOpenStateListener,
                 final Consumer<CircuitBreaker> toOpenStateListener) {
    if (threshold < 1) {
      throw new IllegalArgumentException("Failure threshold should be greater than 0");
    }
    if (timeout < 1) {
      throw new IllegalArgumentException("Timeout should be greater than 0");
    }
    this.name = name;
    this.threshold = threshold;
    this.timeout = timeout;
    this.toClosedStateListener = toClosedStateListener;
    this.toHalfOpenStateListener = toHalfOpenStateListener;
    this.toOpenStateListener = toOpenStateListener;
    moveToClosedState();
  }

  public Try<Void> attempt(final TryRunnable protectedCode) {
    return attempt(LazyTry.ofFailable(protectedCode));
  }

  public <T> Try<T> attempt(final TrySupplier<T> protectedCode) {
    return attempt(LazyTry.ofFailable(protectedCode));
  }

  public <T> Try<T> attempt(final LazyTry<T> protectedCode) {
    synchronized (monitor) {
      update();
      if (State.OPEN.equals(state)) {
        return Try.failure(new CircuitBreakerOpenException("Circuit breaker open, attempt aborted", lastObservedFailure));
      }
    }

    this.lastObservedFailure = null;

    T result;

    try {
      result = protectedCode.run().get();
    } catch (Throwable t) {
      lastObservedFailure = t;
      synchronized (monitor) {
        failures++;
        if ((State.CLOSED.equals(state) && isThresholdReached()) || State.HALF_OPEN.equals(state)) {
          moveToOpenState();
        }
      }
      return Try.failure(t);
    }

    synchronized (monitor) {
      if (State.HALF_OPEN.equals(state)) {
        moveToClosedState();
      }
    }

    return Try.success(result);
  }

  private void update() {
    if (State.OPEN.equals(state) && System.currentTimeMillis() >= openTime + getTimeout()) {
      moveToHalfOpenState();
    }
  }

  public void close() {
    synchronized (monitor) {
      moveToClosedState();
    }
  }

  public void open() {
    synchronized (monitor) {
      moveToOpenState();
    }
  }

  private void moveToClosedState() {
    toClosedStateListener.accept(this);
    state = State.CLOSED;
    failures = 0;
  }

  private void moveToHalfOpenState() {
    toHalfOpenStateListener.accept(this);
    state = State.HALF_OPEN;
  }

  private void moveToOpenState() {
    toOpenStateListener.accept(this);
    state = State.OPEN;
    openTime = System.currentTimeMillis();
  }

  public boolean isClosed() {
    update();
    return State.CLOSED.equals(state);
  }

  public boolean isHalfOpen() {
    update();
    return State.HALF_OPEN.equals(state);
  }

  public boolean isOpen() {
    update();
    return State.OPEN.equals(state);
  }

  public boolean isThresholdReached() {
    return failures >= threshold;
  }

  public Optional<Throwable> getFailureFromLastAttempt() {
    return Optional.ofNullable(lastObservedFailure);
  }

  public String getName() {
    return name;
  }

  public long getTimeout() {
    return timeout;
  }

  public int getThreshold() {
    return threshold;
  }

}
