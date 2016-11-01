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

package io.strati.functional.exception;

import io.strati.functional.resilience.CircuitBreaker;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 */

public class CircuitBreakerOpenException extends RuntimeException {
  private final CircuitBreaker.State state;

  public CircuitBreakerOpenException(final String msg, final Throwable cause, CircuitBreaker.State state) {
    super(msg, cause);
    this.state = state;
  }

  /**
   * @return The state of the circuit breaker when this exception was thrown,
   * either {@link io.strati.functional.resilience.CircuitBreaker.State#OPEN} or
   * {@link io.strati.functional.resilience.CircuitBreaker.State#HALF_OPEN}
   */
  public CircuitBreaker.State getState() {
    return state;
  }
}
