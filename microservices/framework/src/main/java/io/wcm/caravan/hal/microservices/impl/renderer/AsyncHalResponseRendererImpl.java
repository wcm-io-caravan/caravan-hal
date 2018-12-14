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

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.hal.resource.HalResource;


public class AsyncHalResponseRendererImpl implements AsyncHalResponseRenderer {

  static final String CARAVAN_METADATA_RELATION = "caravan:metadata";

  private final AsyncHalResourceRenderer renderer;

  private final RequestMetricsCollector metrics;

  private final VndErrorResponseRenderer errorRenderer;

  public AsyncHalResponseRendererImpl(AsyncHalResourceRenderer renderer, RequestMetricsCollector metrics,
      ExceptionStatusAndLoggingStrategy exceptionStrategy) {
    this.renderer = renderer;
    this.metrics = metrics;
    this.errorRenderer = new VndErrorResponseRendererImpl(exceptionStrategy);
  }

  @Override
  public Single<HalResponse> renderResponse(String requestUri, LinkableResource resourceImpl) {

    return renderer.renderResource(resourceImpl)
        .map(halResource -> createResponse(resourceImpl, halResource))
        .onErrorReturn(ex -> errorRenderer.renderError(resourceImpl, ex, requestUri, metrics));
  }

  HalResponse createResponse(LinkableResource resourceImpl, HalResource halResource) {

    addMetadata(metrics, halResource, resourceImpl);

    HalResponse response = new HalResponse()
        .withStatus(200)
        .withContentType(HalResource.CONTENT_TYPE)
        .withReason("Ok")
        .withBody(halResource)
        .withMaxAge(metrics.getOutputMaxAge());

    return response;
  }

  static void addMetadata(RequestMetricsCollector metrics, HalResource hal, LinkableResource resourceImpl) {

    HalResource metadata = metrics.createMetadataResource(resourceImpl);
    if (metadata != null) {
      hal.addEmbedded(CARAVAN_METADATA_RELATION, metadata);
    }
  }
}
