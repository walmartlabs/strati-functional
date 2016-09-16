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

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A container object which may or may not contain a non-null value.
 * Instances of {@code Optional} are either {@code Present} or {@code Empty}.
 * This is an extended implementation of {@code java.util.Optional} that allows
 * better composition and removes the need of calling {@code get()} and performing
 * imperative checks/actions in most cases.
 */
public abstract class Optional<T> {

  /**
   * Common instance for {@code empty()}.
   */
  private static final Optional<?> EMPTY = new Empty<>();

  /**
   * Convert a {java.util.Optional} into a {io.strati.functional.Optional}.
   *
   * @param jdkOptional the {@code java.util.Optional} to convert
   * @param <T>         Type of the value
   * @return The converted {java.util.Optional} as a {io.strati.functional.Optional}
   */
  public static <T> Optional<T> from(final java.util.Optional<T> jdkOptional) {
    return jdkOptional.isPresent() ? ofNullable(jdkOptional.get()) : empty();
  }

  /**
   * Returns an empty {@code Optional} instance.  No value is present for this
   * Optional.
   *
   * @param <T> Type of the non-existent value
   * @return an empty {@code Optional}
   */
  public static <T> Optional<T> empty() {
    return (Optional<T>) EMPTY;
  }

  /**
   * Returns an {@code Optional} with the specified present non-null value.
   *
   * @param <T>   the class of the value
   * @param value the value to be present, which must be non-null
   * @return an {@code Optional} with the value present
   * @throws NullPointerException if value is null
   */
  public static <T> Optional<T> of(final T value) {
    return new Present<>(value);
  }

  /**
   * Returns an {@code Optional} describing the specified value, if non-null,
   * otherwise returns an empty {@code Optional}.
   *
   * @param <T>   the class of the value
   * @param value the possibly-null value to describe
   * @return an {@code Optional} with a present value if the specified value
   * is non-null, otherwise an empty {@code Optional}
   */
  public static <T> Optional<T> ofNullable(final T value) {
    return value == null ? empty() : of(value);
  }

  /**
   * If a value is present in this {@code Optional}, returns the value,
   * otherwise throws {@code NoSuchElementException}.
   * <p>
   * Note: Try to avoid calling this method as much as possible!
   *
   * @return the non-null value held by this {@code Optional}
   * @throws NoSuchElementException if there is no value present
   * @see Optional#isPresent()
   */
  public abstract T get();

  /**
   * Return {@code true} if there is a value present, otherwise {@code false}.
   *
   * @return {@code true} if there is a value present, otherwise {@code false}
   */
  public abstract boolean isPresent();

  /**
   * Return {@code true} if there is no value present, otherwise {@code false}.
   *
   * @return {@code true} if there is no value present, otherwise {@code false}
   */
  public abstract boolean isEmpty();

  /**
   * If a value is present, invoke the specified consumer with the value,
   * otherwise do nothing.
   * <p>
   * Note: the (backwards compatible) difference with {@code java.util.Optional}
   * here is that {@code ifPresent} returns the current {@code Optional} instead
   * of {@code void}, this is so that expressions don't have to be broken up
   * in order to do side-effects (i.e. perform actions).
   *
   * @param consumer block to be executed if a value is present
   * @return This instance of {@code Optional} will be returned, no mutations will
   * take place.
   * @throws NullPointerException if value is present and {@code consumer} is
   *                              null
   */
  public abstract Optional<T> ifPresent(final Consumer<? super T> consumer);

  /**
   * If no value is present, invoke the specified runnable, otherwise do nothing.
   *
   * @param runnable block to be executed if no value is present
   * @return This instance of {@code Optional} will be returned, no mutations will
   * take place.
   * @throws NullPointerException if no value is present and {@code runnable} is
   *                              null
   */
  public abstract Optional<T> ifEmpty(final Runnable runnable);

  /**
   * If a value is present, and the value matches the given predicate,
   * return an {@code Optional} describing the value, otherwise return an
   * empty {@code Optional}.
   *
   * @param predicate a predicate to apply to the value, if present
   * @return an {@code Optional} describing the value of this {@code Optional}
   * if a value is present and the value matches the given predicate,
   * otherwise an empty {@code Optional}
   * @throws NullPointerException if a value is present and the predicate is null
   */
  public abstract Optional<T> filter(final Predicate<? super T> predicate);

  /**
   * If a value is present, apply the provided mapping function to it,
   * and if the result is non-null, return an {@code Optional} describing the
   * result.  Otherwise return an empty {@code Optional}.
   *
   * @param <U>    The type of the result of the mapping function
   * @param mapper a mapping function to apply to the value, if present
   * @return an {@code Optional} describing the result of applying a mapping
   * function to the value of this {@code Optional}, if a value is present,
   * otherwise an empty {@code Optional}
   * @throws NullPointerException if a value is present and the mapping
   *                              function is null
   */
  public abstract <U> Optional<U> map(final Function<? super T, ? extends U> mapper);

  /**
   * If a value is present, apply the provided {@code Optional}-bearing
   * mapping function to it, return that result, otherwise return an empty
   * {@code Optional}.  This method is similar to {@link #map(Function)},
   * but the provided mapper is one whose result is already an {@code Optional},
   * and if invoked, {@code flatMap} does not wrap it with an additional
   * {@code Optional}.
   *
   * @param <U>    The type parameter to the {@code Optional} returned by
   * @param mapper a mapping function to apply to the value, if present
   *               the mapping function
   * @return the result of applying an {@code Optional}-bearing mapping
   * function to the value of this {@code Optional}, if a value is present,
   * otherwise an empty {@code Optional}
   * @throws NullPointerException if a value is present and the mapping
   *                              function is null
   */
  public abstract <U> Optional<U> flatMap(final Function<? super T, ? extends Optional<? extends U>> mapper);

  /**
   * If no value is present, run the provided supplier function to it,
   * return an {@code Optional} describing the result.
   * Otherwise return this {@code Optional} without change.
   * <p>
   * Note: {@code orElseMap} is like {@code map} but for the empty case.
   *
   * @param mapper
   * @return an {@code Optional} describing the result of running a mapping
   * supplier, if a value is present and non-null, otherwise an
   * empty {@code Optional}
   * @throws NullPointerException if no value is present and the mapping
   *                              function is null
   */
  public abstract Optional<T> orElseMap(final Supplier<? extends T> mapper);

  /**
   * If no value is present, run the provided {@code Optional}-bearing
   * mapping supplier, return that result, otherwise return an empty
   * {@code Optional}.  This method is similar to {@link #orElseMap(Supplier)},
   * but the provided mapper is one whose result is already an {@code Optional},
   * and if invoked, {@code orElseFlatMap} does not wrap it with an additional
   * {@code Optional}.
   * <p>
   * Note: {@code orElseFlatMap} is like {@code flatMap} but for the empty case.
   *
   * @param mapper a mapping supplier to run if no value is present
   * @return the result of running an {@code Optional}-bearing mapping
   * supplier, if no value is present, otherwise an empty {@code Optional}
   * @throws NullPointerException if no value is present and the mapping
   *                              function is null
   */
  public abstract Optional<T> orElseFlatMap(final Supplier<? extends Optional<? extends T>> mapper);

  /**
   * Return the value if present, otherwise return {@code other}.
   *
   * @param other the value to be returned if there is no value present, may
   *              be null
   * @return the value, if present, otherwise {@code other}
   */
  public abstract T orElse(final T other);

  /**
   * Return the value if present, otherwise invoke {@code other} and return
   * the result of that invocation.
   *
   * @param other a {@code Supplier} whose result is returned if no value
   *              is present
   * @return the value if present otherwise the result of {@code other.get()}
   * @throws NullPointerException if value is not present and {@code other} is
   *                              null
   */
  public abstract T orElseGet(final Supplier<? extends T> other);

  /**
   * Return the contained value, if present, otherwise throw an exception
   * to be created by the provided supplier.
   *
   * @param <X>               Type of the exception to be thrown
   * @param exceptionSupplier The supplier which will return the exception to
   *                          be thrown
   * @return the present value
   * @throws X                    if there is no value present
   * @throws NullPointerException if no value is present and
   *                              {@code exceptionSupplier} is null
   */
  public abstract <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X;

  /**
   * @return If a value is present return a singleton {@code Stream<T>} containing
   * the value, otherwise an empty {@code Stream<T>}.
   */
  public abstract Stream<T> stream();

  /**
   * @return If a value is present returns a {@code Success} containing the value,
   * otherwise a {@code Failure} containing a {@code NoSuchElementException}
   */
  public abstract Try<T> toTry();

  /**
   * @return Convert this {@code io.strati.functional.Optional} into a {@code java.util.Optional}
   */
  public abstract java.util.Optional<T> toJdkOptional();

}

/**
 * The Present class is used to create an object bound, not null value holder.
 * Used to guarantee the presence of a value. But should not be used to validate
 * the state or internal representation of the value.
 *
 * @param <T>
 */
final class Present<T> extends Optional<T> {
  private final T value;

  protected Present(final T value) {
    if (value == null) {
      throw new NullPointerException();
    }
    this.value = value;
  }

  @Override
  public T get() {
    return value;
  }

  @Override
  public boolean isPresent() {
    return true;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Optional<T> ifPresent(final Consumer<? super T> consumer) {
    consumer.accept(value);
    return this;
  }

  @Override
  public Optional<T> ifEmpty(final Runnable runnable) {
    return this;
  }

  @Override
  public Optional<T> filter(final Predicate<? super T> predicate) {
    return predicate.test(value) ? of(value) : empty();
  }

  @Override
  public <U> Optional<U> map(final Function<? super T, ? extends U> mapper) {
    return ofNullable(mapper.apply(value));
  }

  @Override
  public <U> Optional<U> flatMap(final Function<? super T, ? extends Optional<? extends U>> mapper) {
    final Optional<U> result = (Optional<U>) mapper.apply(value);
    return result == null ? empty() : result;
  }

  @Override
  public Optional<T> orElseMap(final Supplier<? extends T> mapper) {
    return this;
  }

  @Override
  public Optional<T> orElseFlatMap(final Supplier<? extends Optional<? extends T>> mapper) {
    return this;
  }

  @Override
  public T orElse(final T other) {
    return value;
  }

  @Override
  public T orElseGet(final Supplier<? extends T> other) {
    return value;
  }

  @Override
  public <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X {
    return value;
  }

  @Override
  public Stream<T> stream() {
    return Stream.of(value);
  }

  @Override
  public Try<T> toTry() {
    return Try.success(value);
  }

  @Override
  public java.util.Optional<T> toJdkOptional() {
    return java.util.Optional.of(value);
  }
}

final class Empty<T> extends Optional<T> {

  @Override
  public T get() {
    throw new NoSuchElementException();
  }

  @Override
  public boolean isPresent() {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public Optional<T> ifPresent(final Consumer<? super T> consumer) {
    return this;
  }

  @Override
  public Optional<T> ifEmpty(final Runnable runnable) {
    runnable.run();
    return this;
  }

  @Override
  public Optional<T> filter(final Predicate<? super T> predicate) {
    return this;
  }

  @Override
  public <U> Optional<U> map(final Function<? super T, ? extends U> mapper) {
    return (Optional<U>) this;
  }

  @Override
  public <U> Optional<U> flatMap(final Function<? super T, ? extends Optional<? extends U>> mapper) {
    return (Optional<U>) this;
  }

  @Override
  public Optional<T> orElseMap(final Supplier<? extends T> mapper) {
    return ofNullable(mapper.get());
  }

  @Override
  public Optional<T> orElseFlatMap(final Supplier<? extends Optional<? extends T>> mapper) {
    return (Optional<T>) mapper.get();
  }

  @Override
  public T orElse(final T other) {
    return other;
  }

  @Override
  public T orElseGet(final Supplier<? extends T> other) {
    return other.get();
  }

  @Override
  public <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X {
    throw exceptionSupplier.get();
  }

  @Override
  public Stream<T> stream() {
    return Stream.empty();
  }

  @Override
  public Try<T> toTry() {
    return Try.failure(new NoSuchElementException());
  }

  @Override
  public java.util.Optional<T> toJdkOptional() {
    return java.util.Optional.empty();
  }
}