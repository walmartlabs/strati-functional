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



  public enum State {
    CLOSED, HALF_OPEN, OPEN
  }

  private final String name;
  private final int threshold;
  private final long timeout;

  private final Consumer<CircuitBreaker> toClosedStateListener;
  private final Consumer<CircuitBreaker> toHalfOpenStateListener;
  private final Consumer<CircuitBreaker> toOpenStateListener;

  private final HalfOpenFilter halfOpenFilter;

  private final Object monitor = new Object();

  private State state;
  private int failures = 0;
  private Throwable lastObservedFailure;
  private long openTime;

  protected CircuitBreaker(final String name, final int threshold, final long timeout,
                           final Integer concurrentCallsInHalfOpenState,
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
    this.halfOpenFilter = concurrentCallsInHalfOpenState != null ? new HalfOpenFilter(concurrentCallsInHalfOpenState) : null;
    moveToClosedState();
  }

  /**
   * Execute a {@code TryRunnable} procedure in a protected context.
   *
   * @param protectedCode the procedure that will run in the safe {@code CircuitBreaker} context
   * @return {@code Try<Void>}
   */
  public Try<Void> attempt(final TryRunnable protectedCode) {
    return attempt(LazyTry.ofFailable(protectedCode));
  }

  /**
   * Execute a {@code TrySupplier<T>} in a protected context.
   *
   * @param protectedCode the procedure that will run in the safe {@code CircuitBreaker} context
   * @param <T> return type of the protected procedure
   * @return {@code Try<T>}
   */
  public <T> Try<T> attempt(final TrySupplier<T> protectedCode) {
    return attempt(LazyTry.ofFailable(protectedCode));
  }

  /**
   * Execute a {@code LazyTry<T>} in a protected context.
   *
   * @param protectedCode the procedure that will run in the safe {@code CircuitBreaker} context
   * @param <T> return type of the protected procedure
   * @return {@code Try<T>}
   */
  public <T> Try<T> attempt(final LazyTry<T> protectedCode) {
    synchronized (monitor) {
      update();
      switch (state) {
        case OPEN:
          return Try.failure(new CircuitBreakerOpenException("Circuit breaker open, attempt aborted", lastObservedFailure, state));
        case HALF_OPEN:
          if (halfOpenFilter != null && !halfOpenFilter.enter()) {
            return Try.failure(new CircuitBreakerOpenException("Circuit breaker half-open, attempt aborted", lastObservedFailure, state));
          }
          break;
        case CLOSED:
          // No problem just continue
          break;
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
        switch (state) {
          case CLOSED:
            if (failures >= threshold) {
              moveToOpenState();
            }
            break;
          case HALF_OPEN:
            if (halfOpenFilter != null) {
              halfOpenFilter.exit();
            }
            moveToOpenState();
            break;
        }
      }
      return Try.failure(t);
    }

    synchronized (monitor) {
      if (State.HALF_OPEN.equals(state)) {
        if (halfOpenFilter != null) {
          halfOpenFilter.exit();
        }
        // TODO: Only switch back to closed after a certain number of successes?
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

  /**
   * change the state of the {@code CircuitBreaker} to CLOSED
   */
  public void close() {
    synchronized (monitor) {
      moveToClosedState();
    }
  }

  /**
   * change the state of the {@code CircuitBreaker} to OPEN
   */
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
    if (halfOpenFilter != null) {
      halfOpenFilter.reset();
    }
  }

  /**
   * @return true iff the state of the {@code CircuitBreaker} is CLOSED else false
   */
  public boolean isClosed() {
    update();
    return State.CLOSED.equals(state);
  }

  /**
   * @return true iff the state of the {@code CircuitBreaker} is HALF_OPEN else false
   */
  public boolean isHalfOpen() {
    update();
    return State.HALF_OPEN.equals(state);
  }

  /**
   * @return true iff the state of the {@code CircuitBreaker} is OPEN else false
   */
  public boolean isOpen() {
    update();
    return State.OPEN.equals(state);
  }

  /**
   * @return the (optional) {@code Throwable} from the last attempt that any protected code was run
   */
  public Optional<Throwable> getFailureFromLastAttempt() {
    return Optional.ofNullable(lastObservedFailure);
  }

  /**
   * @return the name of the {@code CircuitBreaker}
   */
  public String getName() {
    return name;
  }

  /**
   * @return the timeout duration (in ms.) of the {@code CircuitBreaker}
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * @return the error threshold of the {@code CircuitBreaker}
   */
  public int getThreshold() {
    return threshold;
  }

  /**
   * @return the current state of the {@code CircuitBreaker}
   */
  public State getState() {
    return state;
  }
}
