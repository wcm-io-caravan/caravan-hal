/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.SingleSubject;
import io.reactivex.subjects.Subject;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;

@RunWith(MockitoJUnitRunner.class)
public class EmissionStopwatchTest {

  private static final String METHOD_DESC = "test";
  @Mock
  private RequestMetricsCollector metrics;

  @Test
  public void should_measure_emission_times_for_single() throws Exception {

    SingleSubject<String> subject = SingleSubject.create();

    subject
        .compose(EmissionStopwatch.collectMetrics(METHOD_DESC, metrics))
        .subscribe();

    Thread.sleep(20);

    subject.onSuccess("item");

    Mockito.verify(metrics).onMethodInvocationFinished(eq(EmissionStopwatch.class), eq(METHOD_DESC), longThat(micros -> micros > 20000));
  }

  @Test
  public void should_measure_emission_times_for_observable() throws Exception {

    Subject<String> subject = PublishSubject.create();

    subject
        .compose(EmissionStopwatch.collectMetrics(METHOD_DESC, metrics))
        .subscribe();

    Thread.sleep(20);

    subject.onNext("item1");
    subject.onComplete();

    Mockito.verify(metrics).onMethodInvocationFinished(eq(EmissionStopwatch.class), eq(METHOD_DESC), longThat(micros -> micros > 20000));
  }

  @Test
  public void should_allow_multiple_subscribers_for_observable() throws Exception {

    SingleSubject<String> subject = SingleSubject.create();

    Single<String> obs = subject.compose(EmissionStopwatch.collectMetrics(METHOD_DESC, metrics));
    obs.subscribe();
    obs.subscribe();

    Thread.sleep(20);

    subject.onSuccess("item");

    Mockito.verify(metrics, Mockito.times(2)).onMethodInvocationFinished(eq(EmissionStopwatch.class), eq(METHOD_DESC), longThat(micros -> micros > 20000));
  }
}
