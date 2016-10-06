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
public class LazyTry<T> {

  private TrySupplier<Try<T>> supplier;

  private LazyTry(final TrySupplier<Try<T>> supplier) {
    this.supplier = supplier;
  }

  public static <T> LazyTry<T> ofTry(final TrySupplier<Try<T>> s) {
    return new LazyTry<>(s);
  }

  public static <T> LazyTry<T> ofFailable(final TrySupplier<T> s) {
    return new LazyTry<>(() -> Try.ofFailable(s));
  }

  public static LazyTry<Void> ofFailable(final TryRunnable r) {
    return new LazyTry<>(() -> Try.ofFailable(r));
  }

  public LazyTry<T> ifSuccess(final TryConsumer<T> f) {
    return new LazyTry<>(() -> supplier.get().ifSuccess(f));
  }

  public LazyTry<T> ifSuccess(final TryRunnable r) {
    return new LazyTry<>(() -> supplier.get().ifSuccess(r));
  }

  public LazyTry<T> ifFailure(final TryConsumer<Throwable> c) {
    return new LazyTry<>(() -> supplier.get().ifFailure(c));
  }

  public <E extends Exception> LazyTry<T> ifFailure(final Class<E> e, final TryConsumer<E> c) {
    return new LazyTry<>(() -> supplier.get().ifFailure(e, c));
  }

  public <U> LazyTry<U> flatMap(final TryFunction<? super T, ? extends Try<? extends U>> f) {
    return new LazyTry<>(() -> supplier.get().flatMap(t -> f.apply(t)));
  }

  public <U> LazyTry<U> flatMap(final TrySupplier<Try<? extends U>> f) {
    return new LazyTry<>(() -> supplier.get().flatMap(f::get));
  }

  public <U> LazyTry<U> map(final TryFunction<? super T, ? extends U> f) {
    return new LazyTry<>(() -> supplier.get().map(f));
  }

  public <U> LazyTry<U> map(final TrySupplier<? extends U> f) {
    return new LazyTry<>(() -> supplier.get().map(f));
  }

  public LazyTry<T> filter(final TryPredicate<? super T> predicate) {
    return new LazyTry<>(() -> supplier.get().filter(predicate));
  }

  public LazyTry<T> filter(final TryPredicate<? super T> predicate, final Throwable t) {
    return new LazyTry<>(() -> supplier.get().filter(predicate, t));
  }

  public LazyTry<T> filter(final TryPredicate<? super T> predicate, final TryFunction<? super T, Try<? extends T>> orElse) {
    return new LazyTry<>(() -> supplier.get().filter(predicate, orElse));
  }

  public LazyTry<T> filter(final TryPredicate<? super T> predicate, final TrySupplier<Try<? extends T>> orElse) {
    return new LazyTry<>(() -> supplier.get().filter(predicate, orElse));
  }

  public LazyTry<T> recoverWith(final TryFunction<Throwable, Try<? extends T>> f) {
    return new LazyTry<>(() -> supplier.get().recoverWith(f));
  }

  public <E extends Exception> LazyTry<T> recoverWith(final Class<E> e, final TryFunction<E, Try<? extends T>> f) {
    return new LazyTry<>(() -> supplier.get().recoverWith(e, f));
  }

  public LazyTry<T> recover(final TryFunction<Throwable, ? extends T> f) {
    return new LazyTry<>(() -> supplier.get().recover(f));
  }

  public <E extends Exception> LazyTry<T> recover(final Class<E> e, final TryFunction<E, ? extends T> f) {
    return new LazyTry<>(() -> supplier.get().recover(e, f));
  }

  public Try<T> run() {
    try {
      return supplier.get();
    } catch (final Exception e) {
      return Try.failure(e);
    }
  }

  public <U> U apply(final Function<LazyTry<T>, ? extends U> f) {
    return f.apply(this);
  }

}
