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
package io.wcm.caravan.hal.microservices.impl.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


class CachingJsonResourceLoader implements JsonResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(CachingJsonResourceLoader.class);

  private final Cache<String, Single<HalResponse>> cache = CacheBuilder.newBuilder().build();

  private final JsonResourceLoader delegate;
  private final RequestMetricsCollector metrics;

  CachingJsonResourceLoader(JsonResourceLoader delegate, RequestMetricsCollector metrics) {
    this.delegate = delegate;
    this.metrics = metrics;
  }

  @Override
  public Single<HalResponse> loadJsonResource(String uri) {
    try {
      return cache.get(uri, () -> {

        Stopwatch stopwatch = Stopwatch.createUnstarted();

        return delegate.loadJsonResource(uri)
            .doOnSubscribe(d -> stopwatch.start())
            .doOnError(ex -> registerErrorMetrics(uri, ex, stopwatch))
            .doOnSuccess(jsonResponse -> registerResponseMetrics(uri, jsonResponse, stopwatch))
            .cache();
      });
    }
    catch (ExecutionException ex) {
      throw new RuntimeException(ex.getCause());
    }
  }

  private void registerErrorMetrics(String uri, Throwable ex, Stopwatch stopwatch) {

    String title = "Upstream resource that failed to load: " + ex.getMessage();

    metrics.onResponseRetrieved(uri, title, null, stopwatch.elapsed(TimeUnit.MICROSECONDS));
  }

  private void registerResponseMetrics(String uri, HalResponse jsonResponse, Stopwatch stopwatch) {

    log.debug("Received JSON response from {} with status code {} and max-age {} in {}ms",
        uri, jsonResponse.getStatus(), jsonResponse.getMaxAge(), stopwatch.elapsed(TimeUnit.MILLISECONDS));

    String title = getResourceTitle(jsonResponse.getBody(), uri);

    metrics.onResponseRetrieved(uri, title, jsonResponse.getMaxAge(), stopwatch.elapsed(TimeUnit.MICROSECONDS));
  }

  private static String getResourceTitle(HalResource halResource, String uri) {

    Link selfLink = halResource.getLink();

    String title = null;
    if (selfLink != null) {
      title = selfLink.getTitle();
    }

    if (title == null) {
      title = "Untitled HAL resource from " + uri;
    }

    return title;
  }

}
