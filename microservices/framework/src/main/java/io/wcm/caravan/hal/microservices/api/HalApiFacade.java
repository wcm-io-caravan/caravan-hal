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
package io.wcm.caravan.hal.microservices.api;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalApiTypeSupport;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.impl.client.HalApiClientImpl;
import io.wcm.caravan.hal.microservices.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResponseRendererImpl;

public class HalApiFacade {

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

  private final AsyncHalResponseRenderer renderer;
  private final HalApiClient client;

  public HalApiFacade(@NonNull JsonResourceLoader jsonLoader) {
    this(jsonLoader, null, new DefaultHalApiTypeSupport());
  }

  public HalApiFacade(JsonResourceLoader jsonLoader, ExceptionStatusAndLoggingStrategy exceptionStrategy, HalApiTypeSupport typeSupport) {

    AsyncHalResourceRenderer resourceRenderer = new AsyncHalResourceRendererImpl(metrics, typeSupport);
    this.renderer = new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy, typeSupport);

    this.client = new HalApiClientImpl(jsonLoader, metrics, typeSupport);
  }

  public <T> T getEntryPoint(String uri, Class<T> halApiInterface) {

    return client.getEntryPoint(uri, halApiInterface);
  }

  public void limitOutputMaxAge(int seconds) {

    metrics.limitOutputMaxAge(seconds);
  }

  public Single<HalResponse> renderResponse(String requestUri, LinkableResource resourceImpl) {

    return renderer.renderResponse(requestUri, resourceImpl);
  }
}
