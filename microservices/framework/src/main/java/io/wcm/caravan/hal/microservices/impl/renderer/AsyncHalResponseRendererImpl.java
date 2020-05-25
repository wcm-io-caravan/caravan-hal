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
package io.wcm.caravan.hal.microservices.impl.renderer;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.microservices.api.common.HalApiTypeSupport;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.hal.microservices.impl.metadata.ResponseMetadataRelations;
import io.wcm.caravan.hal.microservices.impl.reflection.HalApiReflectionUtils;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * A full implementation of {@link AsyncHalResourceRenderer} that uses {@link VndErrorResponseRendererImpl}
 * to handle any errors that occured when rendering the {@link HalResource}
 */
public class AsyncHalResponseRendererImpl implements AsyncHalResponseRenderer {

  private final AsyncHalResourceRenderer renderer;

  private final RequestMetricsCollector metrics;

  private final VndErrorResponseRenderer errorRenderer;

  private final HalApiTypeSupport typeSupport;

  /**
   * @param renderer used to asynchronously render a {@link HalResource}
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance and caching information for
   *          the current incoming request
   * @param exceptionStrategy allows to control the status code and logging of exceptions being thrown during rendering
   * @param typeSupport the strategy to detect HAL API annotations and perform type conversions
   */
  public AsyncHalResponseRendererImpl(AsyncHalResourceRenderer renderer, RequestMetricsCollector metrics,
      ExceptionStatusAndLoggingStrategy exceptionStrategy, HalApiTypeSupport typeSupport) {
    this.renderer = renderer;
    this.metrics = metrics;
    this.errorRenderer = VndErrorResponseRenderer.create(exceptionStrategy);
    this.typeSupport = typeSupport;
  }

  @Override
  public Single<HalResponse> renderResponse(String requestUri, LinkableResource resourceImpl) {

    return renderer.renderResource(resourceImpl)
        .map(halResource -> createResponse(resourceImpl, halResource))
        .onErrorReturn(ex -> errorRenderer.renderError(requestUri, resourceImpl, ex, metrics));
  }

  HalResponse createResponse(LinkableResource resourceImpl, HalResource halResource) {

    addMetadata(metrics, halResource, resourceImpl);

    Class<?> halApiInterface = HalApiReflectionUtils.findHalApiInterface(resourceImpl, typeSupport);
    String contentType = typeSupport.getContentType(halApiInterface);

    HalResponse response = new HalResponse()
        .withStatus(200)
        .withContentType(contentType)
        .withReason("Ok")
        .withBody(halResource)
        .withMaxAge(metrics.getResponseMaxAge());

    return response;
  }

  static void addMetadata(RequestMetricsCollector metrics, HalResource hal, LinkableResource resourceImpl) {

    HalResource metadata = metrics.createMetadataResource(resourceImpl);
    if (metadata != null) {
      hal.addEmbedded(ResponseMetadataRelations.CARAVAN_METADATA_RELATION, metadata);
    }
  }
}
