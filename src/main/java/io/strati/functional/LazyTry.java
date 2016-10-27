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

import io.strati.functional.function.*;

import java.util.function.Function;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 *
 *
 * This is a lazy version of {@code io.strati.functional.Try} that supports lazy composition
 * of potentially failing operations. The same compositional operators that are available for
 * {@code Try} are also available for {@code LazyTry}. The key difference is that with
 * {@code LazyTry} the operations don't actually get executed until the {@code run()} method
 * is invoked. This allows {@code LazyTry} expressions to be chained without actually executing
 * them. This is not directly possible with {@code Try} since it evaluates eagerly.
 * <p>
 * Other than being lazy, all methods have the same semantics on {@code LazyTry} as they have
 * on {@code Try}. For more information regarding specific methods please refer to their
 * {@code Try} counterparts.
 */
@FunctionalInterface
public interface LazyTry<T> {

  Try<T> run();

  static <T> LazyTry<T> success(final T t) {
    return () -> new Success<>(t);
  }

  static <T> LazyTry<T> failure(final Throwable t) {
    return () -> new Failure<>(t);
  }

  static <T> LazyTry<T> ofTry(final TrySupplier<Try<T>> s) {
    return () -> {
      try {
        return s.get();
      } catch (final Exception e) {
        return Try.failure(e);
      }
    };
  }

  static <T> LazyTry<T> ofFailable(final TrySupplier<T> s) {
    return () -> Try.ofFailable(s);
  }

  static LazyTry<Void> ofFailable(final TryRunnable r) {
    return () -> Try.ofFailable(r);
  }

  default LazyTry<T> ifSuccess(final TryConsumer<T> f) {
    return () -> run().ifSuccess(f);
  }

  default LazyTry<T> ifSuccess(final TryRunnable r) {
    return () -> run().ifSuccess(r);
  }

  default LazyTry<T> ifFailure(final TryConsumer<Throwable> c) {
    return () -> run().ifFailure(c);
  }

  default <E extends Exception> LazyTry<T> ifFailure(final Class<E> e, final TryConsumer<E> c) {
    return () -> run().ifFailure(e, c);
  }

  default <U> LazyTry<U> flatMap(final TryFunction<? super T, ? extends Try<? extends U>> f) {
    return () -> run().flatMap(f);
  }

  default <U> LazyTry<U> flatMap(final TrySupplier<Try<? extends U>> f) {
    return () -> run().flatMap(f);
  }

  default <U> LazyTry<U> map(final TryFunction<? super T, ? extends U> f) {
    return () -> run().map(f);
  }

  default <U> LazyTry<U> map(final TrySupplier<? extends U> f) {
    return () -> run().map(f);
  }

  default LazyTry<T> filter(final TryPredicate<? super T> predicate) {
    return () -> run().filter(predicate);
  }

  default LazyTry<T> filter(final TryPredicate<? super T> predicate, final Throwable t) {
    return () -> run().filter(predicate, t);
  }

  default LazyTry<T> filter(final TryPredicate<? super T> predicate, final TryFunction<? super T, Try<? extends T>> orElse) {
    return () -> run().filter(predicate, orElse);
  }

  default LazyTry<T> filter(final TryPredicate<? super T> predicate, final TrySupplier<Try<? extends T>> orElse) {
    return () -> run().filter(predicate, orElse);
  }

  default LazyTry<T> recoverWith(final TryFunction<Throwable, Try<? extends T>> f) {
    return () -> run().recoverWith(f);
  }

  default <E extends Exception> LazyTry<T> recoverWith(final Class<E> e, final TryFunction<E, Try<? extends T>> f) {
    return () -> run().recoverWith(e, f);
  }

  default LazyTry<T> recover(final TryFunction<Throwable, ? extends T> f) {
    return () -> run().recover(f);
  }

  default <E extends Exception> LazyTry<T> recover(final Class<E> e, final TryFunction<E, ? extends T> f) {
    return () -> run().recover(e, f);
  }

  default <U> U apply(final Function<LazyTry<T>, ? extends U> f) {
    return f.apply(this);
  }

}
