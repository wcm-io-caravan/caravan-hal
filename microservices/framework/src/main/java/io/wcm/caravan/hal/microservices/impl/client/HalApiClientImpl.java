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

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;

public class HalApiClientImpl implements HalApiClient {

  private final JsonResourceLoader jsonLoader;
  private final BinaryResourceLoader binaryLoader;
  private final RequestMetricsCollector collector;

  /**
   * @param jsonLoader
   * @param binaryLoader
   * @param collector an object to track all HAL resources that have been fetched while processing the current incoming
   *          request
   */
  public HalApiClientImpl(JsonResourceLoader jsonLoader, BinaryResourceLoader binaryLoader, RequestMetricsCollector collector) {
    this.jsonLoader = jsonLoader;
    this.binaryLoader = binaryLoader;
    this.collector = collector;
  }

  @Override
  public <T> Single<T> getEntryPoint(String uri, Class<T> halApiInterface) {
    // TODO: Auto-generated method stub
    return null;
  }

}
