/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.hal.microservices.util;

import java.util.concurrent.Semaphore;

import io.reactivex.Observable;

/**
 * An alternative to Observable#cache() that will not replay any exceptions that have been thrown.
 * Copied from https://stackoverflow.com/questions/33195722/observable-retry-on-error-and-cache-only-if-completed
 * @param <T>
 */
public class OnErrorRetryCache<T> {

  public static <T> Observable<T> from(Observable<T> source) {
    return new OnErrorRetryCache<>(source).deferred;
  }

  private final Observable<T> deferred;
  private final Semaphore singlePermit = new Semaphore(1);

  private Observable<T> cache = null;
  private Observable<T> inProgress = null;

  private OnErrorRetryCache(Observable<T> source) {
    deferred = Observable.defer(() -> createWhenObserverSubscribes(source));
  }

  private Observable<T> createWhenObserverSubscribes(Observable<T> source) {
    singlePermit.acquireUninterruptibly();

    Observable<T> cached = cache;
    if (cached != null) {
      singlePermit.release();
      return cached;
    }

    inProgress = source
        .doOnComplete(this::onSuccess)
        .doOnTerminate(this::onTermination)
        .replay()
        .autoConnect();

    return inProgress;
  }

  private void onSuccess() {
    cache = inProgress;
  }

  private void onTermination() {
    inProgress = null;
    singlePermit.release();
  }
}
