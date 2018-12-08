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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;


class CachingJsonResourceLoader implements JsonResourceLoader {

  private final Cache<String, Single<JsonResponse>> cache = CacheBuilder.newBuilder().build();

  private final JsonResourceLoader delegate;

  CachingJsonResourceLoader(JsonResourceLoader delegate) {
    this.delegate = delegate;
  }

  @Override
  public Single<JsonResponse> loadJsonResource(String uri, RequestMetricsCollector metrics) {
    try {
      return cache.get(uri, () -> {
        return delegate.loadJsonResource(uri, metrics).cache();
      });
    }
    catch (ExecutionException ex) {
      throw new RuntimeException(ex.getCause());
    }
  }

}
