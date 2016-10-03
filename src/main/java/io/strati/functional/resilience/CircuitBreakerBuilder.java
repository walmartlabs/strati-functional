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

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 */
public class CircuitBreakerBuilder {

  public static final int DEFAULT_THRESHOLD = 3;
  public static final long DEFAULT_TIMEOUT = 3000;

  private String name;
  private int threshold;
  private long timeout;

  private Runnable toClosedStateListener;
  private Runnable toHalfOpenStateListener;
  private Runnable toOpenStateListener;

  public static CircuitBreakerBuilder create() {
    return new CircuitBreakerBuilder();
  }

  public static CircuitBreakerBuilder create(final String name) {
    final CircuitBreakerBuilder cb = new CircuitBreakerBuilder();
    cb.name = name;
    return cb;
  }

  public CircuitBreakerBuilder name(final String name) {
    this.name = name;
    return this;
  }

  public CircuitBreakerBuilder threshold(final int threshold) {
    this.threshold = threshold;
    return this;
  }

  public CircuitBreakerBuilder timeout(final long timeout) {
    this.timeout = timeout;
    return this;
  }

  public CircuitBreakerBuilder timeout(final Duration timeout) {
    this.timeout = timeout.toMillis();
    return this;
  }

  public CircuitBreakerBuilder toClosedStateListener(final Runnable listener) {
    this.toClosedStateListener = listener;
    return this;
  }

  public CircuitBreakerBuilder toHalfOpenStateListener(final Runnable listener) {
    this.toHalfOpenStateListener = listener;
    return this;
  }

  public CircuitBreakerBuilder toOpenStateListener(final Runnable listener) {
    this.toOpenStateListener = listener;
    return this;
  }

  public CircuitBreakerBuilder stateChangeListener(final Runnable listener) {
    this.toClosedStateListener = listener;
    this.toHalfOpenStateListener = listener;
    this.toOpenStateListener = listener;
    return this;
  }

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

    final CircuitBreaker cb = new CircuitBreaker(name, threshold, timeout);

    if (toClosedStateListener != null) {
      cb.setToClosedStateListener(toClosedStateListener);
    }
    if (toHalfOpenStateListener != null) {
      cb.setToHalfOpenStateListener(toHalfOpenStateListener);
    }
    if (toOpenStateListener != null) {
      cb.setToOpenStateListener(toOpenStateListener);
    }

    return cb;
  }

}
