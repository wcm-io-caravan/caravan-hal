/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.hal.microservices.impl.metadata;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.schedulers.Schedulers;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;

/**
 * An alternative to {@link EmissionStopwatch} that is caching the source observables, to avoid that the measured
 * times include the time spent in other observer methods.
 * @param <T> type of the emitted objects
 */
public final class CachingEmissionStopwatch<T> implements SingleTransformer<T, T>, ObservableTransformer<T, T> {

  private final Stopwatch stopwatch = Stopwatch.createUnstarted();

  private final RequestMetricsCollector metrics;
  private final String message;

  private CachingEmissionStopwatch(RequestMetricsCollector metrics, String message) {
    this.metrics = metrics;
    this.message = message;
  }

  /**
   * @param message describes the task that was executed
   * @param metrics to collect the emisison times
   * @return a Transformer to use with {@link Single#compose(SingleTransformer)} or
   *         {@link Observable#compose(ObservableTransformer)}
   */
  public static <T> CachingEmissionStopwatch<T> collectMetrics(String message, RequestMetricsCollector metrics) {
    return new CachingEmissionStopwatch<T>(metrics, message);
  }

  @Override
  public SingleSource<T> apply(Single<T> upstream) {

    Single<T> cachedUpstream = upstream.cache();

    return cachedUpstream.doOnSubscribe(d -> {
      if (!stopwatch.isRunning()) {
        startStopwatch();
        cachedUpstream
            .observeOn(Schedulers.computation())
            .subscribe(this::sendMetrics);
      }
    });
  }

  @Override
  public ObservableSource<T> apply(Observable<T> upstream) {

    Observable<T> cachedUpstream = upstream.cache();

    return cachedUpstream.doOnSubscribe(d -> {
      if (!stopwatch.isRunning()) {
        startStopwatch();
        cachedUpstream
            .observeOn(Schedulers.computation())
            .toList()
            .subscribe(this::sendMetrics);
      }
    });
  }

  private void startStopwatch() {
    if (!stopwatch.isRunning()) {
      stopwatch.start();
    }
  }

  private void sendMetrics(Object o) {
    metrics.onMethodInvocationFinished(EmissionStopwatch.class, message, stopwatch.elapsed(TimeUnit.MICROSECONDS));
  }
}
