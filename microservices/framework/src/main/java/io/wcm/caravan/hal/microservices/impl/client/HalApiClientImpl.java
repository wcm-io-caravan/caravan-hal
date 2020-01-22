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

import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;

/**
 * A full implementation of {@link HalApiClientImpl} that delegates the actual loading of resources via the
 * {@link JsonResourceLoader} interface
 */
public class HalApiClientImpl implements HalApiClient {

  private final CachingJsonResourceLoader jsonLoader;
  private final RequestMetricsCollector metrics;

  /**
   * jsonLoader implements the actual loading (and caching) of JSON/HAL resources via any HTTP client library
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance relevant data for the current
   *          incoming request
   */
  public HalApiClientImpl(JsonResourceLoader jsonLoader, RequestMetricsCollector metrics) {
    this.jsonLoader = new CachingJsonResourceLoader(jsonLoader, metrics);
    this.metrics = metrics;
  }

  @Override
  public <T> T getEntryPoint(String uri, Class<T> halApiInterface) {

    HalApiClientProxyFactory factory = new HalApiClientProxyFactory(jsonLoader, metrics);

    // create a proxy instance that loads the entry point lazily when required by any method call on the proxy
    return factory.createProxyFromUrl(halApiInterface, uri);
  }

}