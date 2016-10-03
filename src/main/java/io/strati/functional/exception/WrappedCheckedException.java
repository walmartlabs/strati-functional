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

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 *
 *
 * This class acts as a wrapper around checked exceptions
 * utilized by the Try and LazyTry classes to create a unified user experiences
 * while using the APIs.
 */
public class WrappedCheckedException extends RuntimeException {
  /**
   * Default constuctor ensures the cause is captured correctly.
   * @param cause the throwable exception to be wrapped.
   */
  public WrappedCheckedException(final Throwable cause) {
    super(cause);
  }
}
