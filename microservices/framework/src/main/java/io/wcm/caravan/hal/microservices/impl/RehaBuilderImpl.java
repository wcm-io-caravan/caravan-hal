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
import java.util.ArrayList;
import java.util.List;

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
import io.wcm.caravan.hal.microservices.impl.reflection.CompositeHalApiTypeSupport;
import io.wcm.caravan.hal.microservices.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResponseRendererImpl;
import io.wcm.caravan.hal.resource.Link;

public class RehaBuilderImpl implements RehaBuilder {

  private final JsonResourceLoader jsonLoader;

  private final List<HalApiTypeSupport> typeSupportRegistry = new ArrayList<>();

  private ExceptionStatusAndLoggingStrategy exceptionStrategy;

  public RehaBuilderImpl(JsonResourceLoader jsonLoader) {
    this.jsonLoader = jsonLoader;

    this.typeSupportRegistry.add(new DefaultHalApiTypeSupport());
  }

  @Override
  public RehaBuilder withTypeSupport(HalApiTypeSupport additionalTypeSupport) {
    typeSupportRegistry.add(additionalTypeSupport);
    return this;
  }

  @Override
  public RehaBuilder withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy) {
    if (exceptionStrategy != null) {
      throw new HalApiDeveloperException("#withExceptionStrategy can only be called once");
    }
    exceptionStrategy = customStrategy;
    return this;
  }

  @Override
  public Reha buildForRequestTo(String requestUri) {

    HalApiTypeSupport typeSupport;
    if (typeSupportRegistry.size() == 1) {
      typeSupport = typeSupportRegistry.get(0);
    }
    else {
      typeSupport = new CompositeHalApiTypeSupport(typeSupportRegistry);
    }

    return new RehaImpl(requestUri, typeSupport);
  }

  private final class RehaImpl implements Reha {

    private final RequestMetricsCollector metrics = RequestMetricsCollector.create();
    private final String requestUri;

    private final HalApiClient client;
    private final AsyncHalResponseRenderer renderer;

    private RehaImpl(String requestUri, HalApiTypeSupport typeSupport) {
      this.requestUri = requestUri;
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
    public Single<HalResponse> respondWith(LinkableResource resourceImpl) {
      return renderer.renderResponse(requestUri, resourceImpl);
    }

    @Override
    public Single<HalResponse> renderVndErrorResource(String requestUri, Throwable error) {

      VndErrorResponseRenderer errorRenderer = VndErrorResponseRenderer.create(exceptionStrategy);
      LinkableResource resourceImpl = new LinkableResource() {

        @Override
        public Link createLink() {
          return null;
        }
      };
      return Single.just(errorRenderer.renderError(requestUri, resourceImpl, error, metrics));
    }
  }
}
