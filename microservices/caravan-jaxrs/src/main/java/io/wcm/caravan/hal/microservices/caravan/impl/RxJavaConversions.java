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
package io.wcm.caravan.hal.microservices.caravan.impl;

public class RxJavaConversions {

  /**
   * Converts an 1.x Single into a 3.x Single, composing cancellation (unsubscription) through.
   * <dl>
   * <dt><b>Scheduler:</b></dt>
   * <dd>The method does not operate by default on a particular {@code Scheduler}.</dd>
   * </dl>
   * @param <T> the value type
   * @param source the source 1.x Single instance, not null
   * @return the new 3.x Single instance
   * @throws NullPointerException if {@code source} is null
   */
  @io.reactivex.rxjava3.annotations.SchedulerSupport(io.reactivex.rxjava3.annotations.SchedulerSupport.NONE)
  static <T> io.reactivex.rxjava3.core.Single<T> toV3Single(rx.Single<T> source) {
    java.util.Objects.requireNonNull(source, "source is null");
    return new SingleV1ToSingleV3<>(source);
  }

  /**
   * Convert a V1 Single into a V3 Single, composing cancellation.
   * @param <T> the value type
   */
  private final static class SingleV1ToSingleV3<T> extends io.reactivex.rxjava3.core.Single<T> {

    final rx.Single<T> source;

    SingleV1ToSingleV3(rx.Single<T> source) {
      this.source = source;
    }

    @Override
    protected void subscribeActual(io.reactivex.rxjava3.core.SingleObserver<? super T> observer) {
      SourceSingleSubscriber<T> parent = new SourceSingleSubscriber<>(observer);
      observer.onSubscribe(parent);
      source.subscribe(parent);
    }

    static final class SourceSingleSubscriber<T> extends rx.SingleSubscriber<T>
        implements io.reactivex.rxjava3.disposables.Disposable {

      final io.reactivex.rxjava3.core.SingleObserver<? super T> observer;

      SourceSingleSubscriber(io.reactivex.rxjava3.core.SingleObserver<? super T> observer) {
        this.observer = observer;
      }

      @Override
      public void onSuccess(T value) {
        if (value == null) {
          observer.onError(new NullPointerException(
              "The upstream 1.x Single signalled a null value which is not supported in 3.x"));
        }
        else {
          observer.onSuccess(value);
        }
      }

      @Override
      public void onError(Throwable error) {
        observer.onError(error);
      }

      @Override
      public void dispose() {
        unsubscribe();
      }

      @Override
      public boolean isDisposed() {
        return isUnsubscribed();
      }
    }
  }
}
