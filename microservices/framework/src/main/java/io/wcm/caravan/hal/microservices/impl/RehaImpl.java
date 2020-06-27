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
package io.wcm.caravan.hal.microservices.impl;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.microservices.api.Reha;
import io.wcm.caravan.hal.microservices.api.RehaBuilder;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.HalApiDeveloperException;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalApiTypeSupport;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.hal.microservices.impl.client.HalApiClientImpl;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResponseRendererImpl;
import io.wcm.caravan.hal.resource.Link;

final class RehaImpl implements Reha {

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

  private final String requestUri;
  private final JsonResourceLoader jsonLoader;
  private final ExceptionStatusAndLoggingStrategy exceptionStrategy;

  private final HalApiClient client;
  private final AsyncHalResponseRenderer renderer;

  RehaImpl(String requestUri, JsonResourceLoader jsonLoader, ExceptionStatusAndLoggingStrategy exceptionStrategy, HalApiTypeSupport typeSupport) {
    this.requestUri = requestUri;
    this.jsonLoader = jsonLoader;
    this.exceptionStrategy = exceptionStrategy;
    this.client = createHalApiClient(typeSupport);
    this.renderer = createResponseRenderer(typeSupport);
  }

  private HalApiClient createHalApiClient(HalApiTypeSupport typeSupport) {

    if (jsonLoader == null) {
      return new HalApiClient() {

        @Override
        public <T> T getEntryPoint(String uri, Class<T> halApiInterface) {
          throw new HalApiDeveloperException("#getEntryPoint can only be used if you have provided a " + JsonResourceLoader.class.getSimpleName()
              + " when constructing your " + RehaBuilder.class.getSimpleName());
        }
      };
    }

    return new HalApiClientImpl(jsonLoader, metrics, typeSupport);
  }

  private AsyncHalResponseRenderer createResponseRenderer(HalApiTypeSupport typeSupport) {

    AsyncHalResourceRenderer resourceRenderer = new AsyncHalResourceRendererImpl(metrics, typeSupport);

    return new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy, typeSupport);
  }

  @Override
  public <T> T getEntryPoint(String uri, Class<T> halApiInterface) {

    return client.getEntryPoint(uri, halApiInterface);
  }

  @Override
  public void setResponseMaxAge(Duration duration) {

    metrics.setResponseMaxAge(duration);
  }

  @Override
  public CompletionStage<HalResponse> renderResponseAsync(LinkableResource resourceImpl) {

    return respondWith(resourceImpl).toCompletionStage();
  }

  @Override
  public HalResponse renderResponse(LinkableResource resourceImpl) {

    return respondWith(resourceImpl).blockingGet();
  }

  @Override
  public HalResponse renderVndErrorResponse(Throwable error) {

    VndErrorResponseRenderer errorRenderer = VndErrorResponseRenderer.create(exceptionStrategy);

    LinkableResource resourceImpl = new LinkableResource() {

      @Override
      public Link createLink() {
        return null;
      }
    };

    return errorRenderer.renderError(requestUri, resourceImpl, error, metrics);
  }

  @Override
  public Single<HalResponse> respondWith(LinkableResource resourceImpl) {
    return renderer.renderResponse(requestUri, resourceImpl);
  }

  @Override
  public Single<HalResponse> renderVndErrorResource(String requestUri, Throwable error) {

    return Single.just(renderVndErrorResponse(error));
  }

}