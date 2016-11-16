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

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 *
 * Builder that is used to instantiate a {@code CircuitBreaker}.
 *
 * Example:
 *
 * CircuitBreaker cb = CircuitBreakerBuilder.create()
 *   .threshold(13)
 *   .timeout(1337)
 *   .stateChangeListener(c -&gt; logger.info(c.getState().name()))
 *   .build();
 */
public class CircuitBreakerBuilder {

  public static final int DEFAULT_THRESHOLD = 3;
  public static final long DEFAULT_TIMEOUT = 3000;

  private String name;
  private int threshold;
  private long timeout;

  private Consumer<CircuitBreaker> toClosedStateListener;
  private Consumer<CircuitBreaker> toHalfOpenStateListener;
  private Consumer<CircuitBreaker> toOpenStateListener;
  private Integer concurrentCallsInHalfOpenState = null;

  /**
   * Instantiate a new {@code CircuitBreakerBuilder}.
   *
   * @return a new {@code CircuitBreakerBuilder} instance
   */
  public static CircuitBreakerBuilder create() {
    return new CircuitBreakerBuilder();
  }

  /**
   * Instantiate a new {@code CircuitBreakerBuilder} with a given name.
   *
   * @param name the name to use for the {@code CircuitBreaker}
   * @return a new {@code CircuitBreakerBuilder} instance
   */
  public static CircuitBreakerBuilder create(final String name) {
    final CircuitBreakerBuilder cb = new CircuitBreakerBuilder();
    cb.name = name;
    return cb;
  }

  /**
   * Set the name to be used for the {@code CircuitBreaker}.
   *
   * @param name the name of the {@code CircuitBreaker}
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder name(final String name) {
    this.name = name;
    return this;
  }

  /**
   * Set the threshold of the {@code CircuitBreaker}. The threshold defines the number of errors that are required
   * for the {@code CircuitBreaker} to "trip" (i.e transition from the CLOSED to the OPEN state).
   *
   * @param threshold the error threshold
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder threshold(final int threshold) {
    this.threshold = threshold;
    return this;
  }

  /**
   * Set the number of concurrent calls of the protected code if the circuit breaker in half open mode.
   *
   * @param concurrentCallsInHalfOpenState the maximum number of concurrent calls when the
   *                                       circuit breaker is in half open state.
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder concurrentCallsInHalfOpenState(final int concurrentCallsInHalfOpenState) {
    this.concurrentCallsInHalfOpenState = concurrentCallsInHalfOpenState;
    return this;
  }

  /**
   * Set the timeout for the {@code CircuitBreaker}. The timeout defines the amount of time before the
   * {@code CircuitBreaker} can transition from the OPEN state to the HALF_OPEN state.
   *
   * @param timeout in milliseconds
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder timeout(final long timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Set the timeout for the {@code CircuitBreaker}. The timeout defines the amount of time before the
   * {@code CircuitBreaker} can transition from the OPEN state to the HALF_OPEN state.
   *
   * @param timeout in Duration
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder timeout(final Duration timeout) {
    this.timeout = timeout.toMillis();
    return this;
  }

  /**
   * Register a callback that is called whenever the {@code CircuitBreaker} transitions to the CLOSED state.
   *
   * @param listener the {@code Runnable} callback
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder toClosedStateListener(final Runnable listener) {
    return toClosedStateListener(c -> listener.run());
  }

  /**
   * Register a callback that is called whenever the {@code CircuitBreaker} transitions to the CLOSED state.
   *
   * @param listener the {@code Consumer<CircuitBreaker>} callback
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder toClosedStateListener(final Consumer<CircuitBreaker> listener) {
    this.toClosedStateListener = listener;
    return this;
  }

  /**
   * Register a callback that is called whenever the {@code CircuitBreaker} transitions to the HALF_OPEN state.
   *
   * @param listener the {@code Runnable} callback
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder toHalfOpenStateListener(final Runnable listener) {
    return toHalfOpenStateListener(c -> listener.run());
  }

  /**
   * Register a callback that is called whenever the {@code CircuitBreaker} transitions to the HALF_OPEN state.
   *
   * @param listener the {@code Consumer<CircuitBreaker>} callback
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder toHalfOpenStateListener(final Consumer<CircuitBreaker> listener) {
    this.toHalfOpenStateListener = listener;
    return this;
  }

  /**
   * Register a callback that is called whenever the {@code CircuitBreaker} transitions to the OPEN state.
   *
   * @param listener the {@code Runnable} callback
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder toOpenStateListener(final Runnable listener) {
    return toOpenStateListener(c -> listener.run());
  }

  /**
   * Register a callback that is called whenever the {@code CircuitBreaker} transitions to the OPEN state.
   *
   * @param listener the {@code Consumer<CircuitBreaker>} callback
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder toOpenStateListener(final Consumer<CircuitBreaker> listener) {
    this.toOpenStateListener = listener;
    return this;
  }

  /**
   * Register a single callback that will be called when the {@code CircuitBreaker} makes any state transition.
   * Notice: this method overrides that callbacks registered for specific state transitions!
   *
   * @param listener the {@code Runnable} callback
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder stateChangeListener(final Runnable listener) {
    return stateChangeListener(c -> listener.run());
  }

  /**
   * Register a single callback that will be called when the {@code CircuitBreaker} makes any state transition.
   * Notice: this method overrides that callbacks registered for specific state transitions!
   *
   * @param listener the {@code Consumer<CircuitBreaker>} callback
   * @return {@code CircuitBreakerBuilder}
   */
  public CircuitBreakerBuilder stateChangeListener(final Consumer<CircuitBreaker> listener) {
    this.toClosedStateListener = listener;
    this.toHalfOpenStateListener = listener;
    this.toOpenStateListener = listener;
    return this;
  }

  /**
   * Build the actual {@code CircuitBreaker} using the properties that are set on this {@code CircuitBreakerBuilder}.
   *
   * @return {@code CircuitBreaker}
   */
  public CircuitBreaker build() {
    if (name == null || name.isEmpty()) {
      name = UUID.randomUUID().toString();
    }
    if (threshold < 1) {
      threshold = DEFAULT_THRESHOLD;
    }
    if (timeout < 1) {
      timeout = DEFAULT_TIMEOUT;
    }
    return new CircuitBreaker(name, threshold, timeout, concurrentCallsInHalfOpenState,
        getOrCreateListener(toClosedStateListener),
        getOrCreateListener(toHalfOpenStateListener),
        getOrCreateListener(toOpenStateListener));
  }

  private static Consumer<CircuitBreaker> getOrCreateListener(final Consumer<CircuitBreaker> listener) {
    return listener != null ? listener : cb -> {};
  }

}
