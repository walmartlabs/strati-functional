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

import io.strati.functional.exception.WrappedCheckedException;
import io.strati.functional.function.*;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author WalmartLabs
 * @author Georgi Khomeriki [gkhomeriki@walmartlabs.com]
 *
 *
 * Implementation of the Try error-handling abstraction, based on the implementation in the Scala Standard Library
 * which in turn is based on Twitter's original implementation in com.twitter.util.
 * <p>
 * The {@code Try} type represents a computation that may either result in an exception, or return a
 * successfully computed value. It's similar to, but semantically different from the {@code java.util.Optional} type.
 * <p>
 * Instances of {@code Try<T>}, are either an instance of {@code Success<T>} or {@code Failure<T>}.
 * <p>
 * For example, {@code Try} can be used to perform code that might throw Exceptions, without the need to do explicit
 * exception-handling in all of the places that an exception might occur.
 * <p>
 * An important property of {@code Try} is its ability to ''pipeline'', or chain, operations, catching exceptions
 * along the way. For example, the {@code flatMap} and {@code map} combinators each essentially pass off either their
 * successfully completed value, wrapped in the `Success` type for it to be further operated
 * upon by the next combinator in the chain, or the exception wrapped in the {@code Failure} type usually to be simply
 * passed on down the chain. Combinators such as {@code recover} and {@code recoverWith} are designed to provide some
 * type of default behavior in the case of failure.
 */
public abstract class Try<T> {

  /**
   * Factory method to wrap values in a Success.
   *
   * @param t   the value to wrap
   * @param <T> type of the payload
   * @return {@code Try<T>}
   */
  public static <T> Try<T> success(final T t) {
    return new Success<>(t);
  }

  /**
   * Factory method to wrap Throwable's in a Failure.
   *
   * @param t   the throwable to wrap
   * @param <T> type of the payload
   * @return {@code Try<T>}
   */
  public static <T> Try<T> failure(final Throwable t) {
    return new Failure<>(t);
  }

  /**
   * If the given Try is a Failure its type parameter will be cast to T, otherwise a ClassCastException will be thrown.
   *
   * @param t the {@code Failure<T>} whose type parameter will be cast
   * @param <T> the original type parameter
   * @param <U> the resulting type parameter
   * @return {@code Try<U>}
   */
  public static <T, U> Try<U> failure(final Try<T> t) {
    return (Failure<U>) t;
  }

  /**
   * Factory method to create a Try out of a block of (possibly) throwing code.
   *
   * @param supplier supplier of the value for the Try which possibly throws
   * @param <T>      type of the payload
   * @return {@code Success<T>} if supplier returns a value, if it throws then a {@code Failure<T>}
   */
  public static <T> Try<T> ofFailable(final TrySupplier<T> supplier) {
    try {
      return success(supplier.get());
    } catch (final Exception e) {
      return failure(e);
    }
  }

  /**
   * Factory method to create a Try out of a block of (possibly) throwing code.
   *
   * @param runnable action to perform which possibly throws
   * @return {@code Success<Void>} if runnable succeeds, if it throws then a {@code Failure<Void>}
   */
  public static Try<Void> ofFailable(final TryRunnable runnable) {
    try {
      runnable.run();
      return success(null);
    } catch (final Exception e) {
      return failure(e);
    }
  }

  /**
   * @param <T> type of the payloads
   * @return Stream Collector that transforms a {@code Stream<Try<T>>} into a {@code Try<List<T>>}.
   * Iff all {@code Try's} in the input stream are a {@code Success} their values will be collected and
   * returned as a {@code Success<List<T>>}. If the input {@code Stream} contains a {@code Failure}, the output
   * will be a {@code Failure} as well.
   */
  public static <T> Collector<Try<T>, AtomicReference<Try<Stream.Builder<T>>>, Try<List<T>>> listCollector() {
    return new TryCollector<T, List<T>>() {
      @Override
      public Supplier<Collector<T, ?, List<T>>> collector() {
        return Collectors::toList;
      }
    };
  }

  /**
   * @param <T> type of the payloads
   * @return Stream Collector that transforms a {@code Stream<Try<T>>} into a {@code Try<Set<T>>}
   * Iff all {@code Try's} in the input stream are a {@code Success} their values will be collected and
   * returned as a {@code Success<Set<T>>}. If the input {@code Stream} contains a {@code Failure}, the output
   * will be a {@code Failure} as well.
   */
  public static <T> Collector<Try<T>, AtomicReference<Try<Stream.Builder<T>>>, Try<Set<T>>> setCollector() {
    return new TryCollector<T, Set<T>>() {
      @Override
      public Supplier<Collector<T, ?, Set<T>>> collector() {
        return Collectors::toSet;
      }
    };
  }

  /**
   * @return {@code true} if the {@code Try} is a {@code Failure}, {@code false} otherwise.
   */
  public abstract boolean isFailure();

  /**
   * @return {@code true} if the {@code Try} is a {@code Success}, {@code false} otherwise.
   */
  public abstract boolean isSuccess();

  /**
   * Returns the value from this {@code Success} or the given {@code defaultValue} if this is a {@code Failure}.
   * Note: This will throw an exception if it is not a success and {@code defaultSupplier} throws an exception.
   *
   * @param defaultValue value to return of this is a {@code Failure}
   * @return T
   */
  public abstract T getOrElse(final T defaultValue);

  /**
   * Returns this {@code Try} if it's a {@code Success} or the given {@code defaultValue} if this is a {@code Failure}.
   *
   * @param defaultTry default {@code Try} value, to be returned if this is {@code Failure}
   * @return {@code Try<T>}
   */
  public abstract Try<T> orElse(final Try<? extends T> defaultTry);

  /**
   * Returns the value from this {@code Success} or throws the exception if this is a {@code Failure}.
   * Try to avoid calling this method as much as possible!
   *
   * @return if this is a success the payload T, otherwise throws an exception.
   */
  public abstract T get();

  /**
   * Applies the given {@code consumer} function iff this is a {@code Success}.
   *
   * @param consumer the function to apply
   * @return the original {@code Try<T>}
   */
  public abstract Try<T> ifSuccess(final TryConsumer<? super T> consumer);

  /**
   * Applies the given {@code consumer} function if this is a {@code Success} and the value is an instance
   * of the given class {@code t}
   *
   * @param consumer the function to apply
   * @param t        the type of the value for which we call the given {@code consumer}
   * @return the original {@code Try<T>}
   */
  public abstract Try<T> ifSuccess(final Class<? extends T> t, final TryConsumer<? super T> consumer);

  /**
   * Executes the given {@code Runnable} if this is a {@code Success} and returns this, otherwise directly returns this.
   *
   * @param runnable the runnable action that performs side-effects
   * @return the original {@code Try<T>}
   */
  public abstract Try<T> ifSuccess(final TryRunnable runnable);

  /**
   * Applies the given {@code consumer} function iff this is a {@code Failure}.
   *
   * @param consumer the function to apply
   * @return the original {@code Try<T>}
   */
  public abstract Try<T> ifFailure(final TryConsumer<Throwable> consumer);

  /**
   * Applies the given {@code consumer} function iff this is a {@code Failure} and the Exception is an instance
   * of the given class {@code e}.
   *
   * @param consumer the function to apply
   * @param e        the class representation of the Exception for which we call the given {@code consumer}
   * @param <E>      the generic type of the Exception
   * @return the original {@code Try<T>}
   */
  public abstract <E extends Exception> Try<T> ifFailure(final Class<E> e, final TryConsumer<E> consumer);

  /**
   * Returns the given function applied to the value from this {@code Success} or returns this if this is a {@code Failure}.
   *
   * @param f   the mapping function
   * @param <U> result type of the mapping
   * @return {@code Try<U>}
   */
  public abstract <U> Try<U> flatMap(final TryFunction<? super T, ? extends Try<? extends U>> f);

  /**
   * Runs the given supplier to transform this {@code Try} if this is a {@code Success} or returns this if this is a {@code Failure}.
   * Note that this method is intended to flatMap functions that discard the value stored in the current {@code Success}.
   *
   * @param f   the mapping function
   * @param <U> result type of the mapping
   * @return {@code Try<U>}
   */
  public abstract <U> Try<U> flatMap(final TrySupplier<? extends Try<? extends U>> f);

  /**
   * Maps the given function to the value from this {@code Success} or returns this if this is a {@code Failure}.
   *
   * @param f   the mapping function
   * @param <U> result type of the mapping
   * @return {@code Try<U>}
   */
  public abstract <U> Try<U> map(final TryFunction<? super T, ? extends U> f);

  /**
   * Runs the given supplier to transform the payload if this is a {@code Success} or returns this if this is a {@code Failure}.
   * Note that this method is intended to map functions that discard the value stored in the current {@code Success}.
   *
   * @param f   the mapping function
   * @param <U> result type of the mapping
   * @return {@code Try<U>}
   */
  public abstract <U> Try<U> map(final TrySupplier<? extends U> f);

  /**
   * Converts this to a {@code Failure} (containing a {@code NoSuchElementException}) if the predicate is not satisfied.
   *
   * @param predicate used to test the payload
   * @return {@code Try<T>}
   */
  public abstract Try<T> filter(final TryPredicate<? super T> predicate);

  /**
   * Converts this to a {@code Failure} (containing the given {@code Throwable}) if the predicate is not satisfied.
   *
   * @param predicate used to test the payload
   * @param t         the {@code Throwable} to use as a payload for the {@code Failure} if the predicate returns false
   * @return {@code Try<T>}
   */
  public abstract Try<T> filter(final TryPredicate<? super T> predicate, final Throwable t);

  /**
   * Converts this using the {@code orElse} {@code Function} if the predicate is not satisfied.
   *
   * @param predicate used to test the payload
   * @param orElse    the function which is used if the predicate isn't satisfied
   * @return {@code Try<T>}
   */
  public abstract Try<T> filter(final TryPredicate<? super T> predicate, final TryFunction<? super T, Try<? extends T>> orElse);

  /**
   * Converts this using the {@code orElse} {@code Supplier} if the predicate is not satisfied.
   *
   * @param predicate used to test the payload
   * @param orElse    the supplier which is used if the predicate isn't satisfied
   * @return {@code Try<T>}
   */
  public abstract Try<T> filter(final TryPredicate<? super T> predicate, final TrySupplier<Try<? extends T>> orElse);

  /**
   * Applies the given function {@code f} if this is a {@code Failure}, otherwise returns this if this is a {@code Success}.
   * {@code recoverWith} is like {@code flatMap} for the {@code Failure} case.
   *
   * @param f the function to apply if this is a {@code Failure}
   * @return {@code Try<T>}
   */
  public abstract Try<T> recoverWith(final TryFunction<Throwable, Try<? extends T>> f);

  /**
   * Applies the given function {@code f} if this is a {@code Failure} and the Exception is an instance of the given
   * class {@code e}, otherwise returns this if this is a {@code Success}.
   * {@code recoverWith} is like {@code flatMap} for the {@code Failure} case.
   *
   * @param f   the function to apply if this is a {@code Failure}
   * @param e   class representing the type of the Exception to recover from
   * @param <E> the type of the Exception to recover from
   * @return {@code Try<T>}
   */
  public abstract <E extends Exception> Try<T> recoverWith(final Class<E> e, final TryFunction<E, Try<? extends T>> f);

  /**
   * Applies the given function {@code f} if this is a {@code Failure}, otherwise returns this if this is a {@code Success}.
   * {@code recover} is like {@code map} for the {@code Failure} case.
   *
   * @param f the function to apply if this is a {@code Failure}
   * @return {@code Try<T>}
   */
  public abstract Try<T> recover(final TryFunction<Throwable, ? extends T> f);

  /**
   * Applies the given function {@code f} if this is a {@code Failure} and the Exception is an instance of the given class {@code e},
   * otherwise returns this if this is a {@code Success}.
   * {@code recover} is like {@code map} for the {@code Failure} case.
   *
   * @param f   the function to apply if this is a {@code Failure}
   * @param e   class representing the type of the Exception to recover from
   * @param <E> the type of the Exception to recover from
   * @return {@code Try<T>}
   */
  public abstract <E extends Exception> Try<T> recover(final Class<E> e, final TryFunction<E, ? extends T> f);

  /**
   * Convert to {@code Optional<T>}
   *
   * @return {@code Optional.empty} if this is a {@code Failure} or an {@code Optional.ofNullable()} containing the
   * value if this is a {@code Success}.
   */
  public abstract Optional<T> toOptional();

  /**
   * Compose sets of common operations on {@code Try}.
   * {@code apply} facilitates re-use of common patterns on {@code Try} without breaking the expression chain.
   * In essence this method is simply wrapping 'function application' to allow a more functional style with chained expressions.
   *
   * @param transformer function that performs the transformation
   * @param <U>         type of the resulting transformation
   * @return result of applying the {@code transformer}. If the {@code transformer} throws, a {@code Failure} will be returned.
   */
  public <U> Try<U> apply(final TryFunction<Try<T>, Try<U>> transformer) {
    try {
      return transformer.apply(this);
    } catch (final Exception e) {
      return failure(e);
    }
  }

}

final class Success<T> extends Try<T> {

  private T value;

  protected Success(final T value) {
    this.value = value;
  }

  @Override
  public boolean isFailure() {
    return false;
  }

  @Override
  public boolean isSuccess() {
    return true;
  }

  @Override
  public T getOrElse(final T defaultValue) {
    return value;
  }

  @Override
  public Try<T> orElse(final Try<? extends T> defaultTry) {
    return this;
  }

  @Override
  public T get() {
    return value;
  }

  @Override
  public Try<T> ifSuccess(final TryConsumer<? super T> consumer) {
    try {
      consumer.accept(value);
      return this;
    } catch (final Exception e) {
      return failure(e);
    }
  }
  
  @Override
  public Try<T> ifSuccess(Class<? extends T> t, TryConsumer<? super T> consumer) {
    try {
      if(t.isInstance(value)){
        consumer.accept(value);
      }
      return this;
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public Try<T> ifSuccess(final TryRunnable runnable) {
    try {
      runnable.run();
      return this;
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public Try<T> ifFailure(final TryConsumer<Throwable> consumer) {
    return this;
  }

  @Override
  public <E extends Exception> Try<T> ifFailure(final Class<E> e, final TryConsumer<E> consumer) {
    return this;
  }

  @Override
  public <U> Try<U> flatMap(TryFunction<? super T, ? extends Try<? extends U>> f) {
    try {
      return (Try<U>) f.apply(value);
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public <U> Try<U> flatMap(final TrySupplier<? extends Try<? extends U>> f) {
    try {
      return (Try<U>) f.get();
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public <U> Try<U> map(final TryFunction<? super T, ? extends U> f) {
    try {
      return success(f.apply(value));
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public <U> Try<U> map(final TrySupplier<? extends U> f) {
    try {
      return success(f.get());
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public Try<T> filter(final TryPredicate<? super T> predicate) {
    try {
      return predicate.test(value) ? this : failure(new NoSuchElementException("Predicate does not hold for " + value));
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public Try<T> filter(final TryPredicate<? super T> predicate, final Throwable t) {
    try {
      return predicate.test(value) ? this : failure(t);
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public Try<T> filter(final TryPredicate<? super T> predicate, final TryFunction<? super T, Try<? extends T>> orElse) {
    try {
      return predicate.test(value) ? this : (Try<T>) orElse.apply(value);
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public Try<T> filter(final TryPredicate<? super T> predicate, final TrySupplier<Try<? extends T>> orElse) {
    try {
      return predicate.test(value) ? this : (Try<T>) orElse.get();
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public Try<T> recoverWith(final TryFunction<Throwable, Try<? extends T>> f) {
    return this;
  }

  @Override
  public <E extends Exception> Try<T> recoverWith(final Class<E> e, final TryFunction<E, Try<? extends T>> f) {
    return this;
  }

  @Override
  public Try<T> recover(final TryFunction<Throwable, ? extends T> f) {
    return this;
  }

  @Override
  public <E extends Exception> Try<T> recover(final Class<E> e, final TryFunction<E, ? extends T> f) {
    return this;
  }

  @Override
  public Optional<T> toOptional() {
    return Optional.ofNullable(value);
  }

}

final class Failure<T> extends Try<T> {

  private Throwable error;

  protected Failure(final Throwable error) {
    this.error = error;
  }

  @Override
  public boolean isFailure() {
    return true;
  }

  @Override
  public boolean isSuccess() {
    return false;
  }

  @Override
  public T getOrElse(final T defaultValue) {
    return defaultValue;
  }

  @Override
  public Try<T> orElse(final Try<? extends T> defaultTry) {
    return (Try<T>) defaultTry;
  }

  @Override
  public T get() {
    throw RuntimeException.class.isInstance(error)
        ? RuntimeException.class.cast(error)
        : new WrappedCheckedException(error);
  }

  @Override
  public Try<T> ifSuccess(final TryConsumer<? super T> consumer) {
    return this;
  }

  @Override
  public Try<T> ifSuccess(TryRunnable runnable) {
    return this;
  }
  
  @Override
  public Try<T> ifSuccess(Class<? extends T> t, TryConsumer<? super T> consumer) {
    return this;
  }

  @Override
  public Try<T> ifFailure(final TryConsumer<Throwable> consumer) {
    try {
      consumer.accept(error);
      return this;
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public <E extends Exception> Try<T> ifFailure(final Class<E> e, final TryConsumer<E> consumer) {
    try {
      if (e.isInstance(error)) {
        consumer.accept((E) error);
      }
      return this;
    } catch (final Exception ex) {
      return failure(ex);
    }
  }

  @Override
  public <U> Try<U> flatMap(final TryFunction<? super T, ? extends Try<? extends U>> f) {
    return (Try<U>) this;
  }

  @Override
  public <U> Try<U> flatMap(TrySupplier<? extends Try<? extends U>> f) {
    return (Try<U>) this;
  }

  @Override
  public <U> Try<U> map(final TryFunction<? super T, ? extends U> f) {
    return (Try<U>) this;
  }

  @Override
  public <U> Try<U> map(TrySupplier<? extends U> f) {
    return (Try<U>) this;
  }

  @Override
  public Try<T> filter(final TryPredicate<? super T> predicate) {
    return this;
  }

  @Override
  public Try<T> filter(final TryPredicate<? super T> predicate, final Throwable t) {
    return this;
  }

  @Override
  public Try<T> filter(final TryPredicate<? super T> predicate, final TryFunction<? super T, Try<? extends T>> orElse) {
    return this;
  }

  @Override
  public Try<T> filter(final TryPredicate<? super T> predicate, final TrySupplier<Try<? extends T>> orElse) {
    return this;
  }

  @Override
  public Try<T> recoverWith(final TryFunction<Throwable, Try<? extends T>> f) {
    try {
      return (Try<T>) f.apply(error);
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public <E extends Exception> Try<T> recoverWith(final Class<E> e, final TryFunction<E, Try<? extends T>> f) {
    try {
      return e.isInstance(error) ? (Try<T>) f.apply((E) error) : this;
    } catch (final Exception ex) {
      return failure(ex);
    }
  }

  @Override
  public Try<T> recover(final TryFunction<Throwable, ? extends T> f) {
    try {
      return success(f.apply(error));
    } catch (final Exception e) {
      return failure(e);
    }
  }

  @Override
  public <E extends Exception> Try<T> recover(final Class<E> e, final TryFunction<E, ? extends T> f) {
    try {
      return e.isInstance(error) ? success(f.apply((E) error)) : this;
    } catch (final Exception ex) {
      return failure(ex);
    }
  }

  @Override
  public Optional<T> toOptional() {
    return Optional.empty();
  }

}

abstract class TryCollector<T, U> implements Collector<Try<T>, AtomicReference<Try<Stream.Builder<T>>>, Try<U>> {
  /**
   * @return A function that, when called, returns a reference to a {@code Success<StreamBuilder>}.
   * The {@code StreamBuilder} will be used to build up the {@code Stream<T>}. The {@code AtomicReference} is needed
   * because the {@code Try} needs to be mutated in certain cases while collecting the {@code Stream} (see {@code accumulator}).
   */
  @Override
  public Supplier<AtomicReference<Try<Stream.Builder<T>>>> supplier() {
    return () -> new AtomicReference<>(Try.success(Stream.builder()));
  }

  /**
   * @return A function that takes a reference to the {@code Try<StreamBuilder>} and the "next" {@code Try<T>},
   * iff both are a {@code Success} we add the payload of "next" to the builder, otherwise we mutate the
   * reference to the {@code StreamBuilder} to a {@code Failure}.
   */
  @Override
  public BiConsumer<AtomicReference<Try<Stream.Builder<T>>>, Try<T>> accumulator() {
    return (builder, next) -> builder.get().ifSuccess(b ->
        next.ifSuccess(b::accept)
            .ifFailure(e -> builder.set(Try.failure(e)))
    );
  }

  /**
   * @return A function that takes two references to {@code Try<StreamBuilder>} and returns a new one in which
   * both input streams are merged. Iff both input streams are a {@code Success}, we add all items from "right"
   * to "left" and return the reference to "left".
   */
  @Override
  public BinaryOperator<AtomicReference<Try<Stream.Builder<T>>>> combiner() {
    return (leftRef, rightRef) -> {
      final Try<Stream.Builder<T>> left = leftRef.get();
      final Try<Stream.Builder<T>> right = rightRef.get();
      leftRef.set(right.flatMap(rb -> left.ifSuccess(lb -> rb.build().forEach(lb::accept))));
      return leftRef;
    };
  }

  /**
   * @return A function that takes a reference to a {@code Try<StreamBuilder>} and returns a {@code Try<U>}
   * iff the input is a {@code Success}. The concrete type of {@code U} is dependent on the {@code Collector} that is
   * supplied by the abstract method {@code collector()}.
   */
  @Override
  public Function<AtomicReference<Try<Stream.Builder<T>>>, Try<U>> finisher() {
    return builder -> builder.get().map(b -> b.build().collect(collector().get()));
  }

  @Override
  public Set<Characteristics> characteristics() {
    return Collections.emptySet();
  }

  /**
   * @return A function that supplies the {@code Collector} to be used in the {@code finisher}.
   */
  public abstract Supplier<Collector<T, ?, U>> collector();
}
