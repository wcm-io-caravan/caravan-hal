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
import io.reactivex.disposables.Disposable;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;

public class EmissionStopwatch<T> implements SingleTransformer<T, T>, ObservableTransformer<T, T> {

  private final Stopwatch stopwatch = Stopwatch.createUnstarted();

  private final RequestMetricsCollector metrics;
  private final String message;

  EmissionStopwatch(RequestMetricsCollector metrics, String message) {
    this.metrics = metrics;
    this.message = message;
  }

  public static <T> EmissionStopwatch<T> collectMetrics(String message, RequestMetricsCollector metrics) {
    return new EmissionStopwatch<T>(metrics, message);
  }

  @Override
  public SingleSource<T> apply(Single<T> upstream) {

    return upstream
        .doOnSubscribe(this::startStopwatch)
        .doOnSuccess(this::sendMetrics);
  }

  @Override
  public ObservableSource<T> apply(Observable<T> upstream) {

    return upstream
        .doOnSubscribe(this::startStopwatch)
        .doOnTerminate(this::sendMetrics);
  }


  private void startStopwatch(Disposable d) {
    if (!stopwatch.isRunning()) {
      stopwatch.start();
    }
  }

  private void sendMetrics(Object o) {
    sendMetrics();
  }

  private void sendMetrics() {
    metrics.onMethodInvocationFinished(EmissionStopwatch.class, message, stopwatch.elapsed(TimeUnit.MICROSECONDS));
  }

}
