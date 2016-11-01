package io.strati.functional.resilience;

import java.util.HashSet;
import java.util.Set;

/**
 * Ensures that only a certain number of Threads execute the protectedAction of a CircuitBreaker.
 */
class HalfOpenFilter {

  private Set<Long> currentThreads = new HashSet<>();

  private int maxConcurrentThreads;

  HalfOpenFilter(final int maxConcurrentThreads) {
    this.maxConcurrentThreads = maxConcurrentThreads;
  }

  /**
   * @return {@code true} iff the current thread may execute the protected action when the CircuitBreaker is
   * currently in half-open state.
   */
  boolean enter() {
    final long currentThreadId = Thread.currentThread().getId();

    if (currentThreads.contains(currentThreadId)) {
      // A recursive call that has already passed the owning CircuitBreaker. Let it pass again.
      return true;
    }

    if (currentThreads.size() < maxConcurrentThreads) {
      currentThreads.add(currentThreadId);
      return true;
    } else {
      return false;
    }

  }

  /**
   * This method must be called when a Thread has called {@link #enter()} and has executed the protectedAction.
   */
  void exit() {
    final long currentThreadId = Thread.currentThread().getId();

    currentThreads.remove(currentThreadId);
  }

  /**
   * Resets this filter when the CircuitBreaker goes into the open state.
   */
  public void reset() {
    currentThreads.clear();
  }
}
