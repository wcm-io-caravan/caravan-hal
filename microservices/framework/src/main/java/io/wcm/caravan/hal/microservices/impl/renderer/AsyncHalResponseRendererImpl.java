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
import io.wcm.caravan.hal.microservices.api.client.JsonResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class AsyncHalResponseRendererImpl implements AsyncHalResponseRenderer {

  private final AsyncHalResourceRenderer renderer;

  private final RequestMetricsCollector metrics;

  public AsyncHalResponseRendererImpl(AsyncHalResourceRenderer renderer, RequestMetricsCollector metrics) {
    this.renderer = renderer;
    this.metrics = metrics;
  }

  @Override
  public Single<JsonResponse> renderResponse(LinkableResource resourceImpl) {

    return renderer.renderResource(resourceImpl)
        .flatMap(hal -> this.createResponse(resourceImpl, hal))
        .onErrorResumeNext(ex -> this.handleError(resourceImpl, ex));
  }

  Single<JsonResponse> createResponse(LinkableResource resourceImpl, HalResource hal) {

    addMetadata(hal, resourceImpl);

    JsonResponse response = new JsonResponse()
        .withStatus(200)
        .withReason("Ok")
        .withBody(hal.getModel())
        .withMaxAge(metrics.getOutputMaxAge());

    return Single.just(response);
  }

  private void addMetadata(HalResource hal, LinkableResource resourceImpl) {

    HalResource metadata = metrics.createMetadataResource(resourceImpl);
    hal.addEmbedded("caravan:metadata", metadata);
  }

  Single<JsonResponse> handleError(LinkableResource resourceImpl, Throwable error) {

    HalResource vndResource = new HalResource();

    addProperties(vndResource, error);
    addEmbeddedCauses(vndResource, error);
    addAboutLink(vndResource, resourceImpl);

    addMetadata(vndResource, resourceImpl);

    JsonResponse response = new JsonResponse()
        .withStatus(500)
        .withBody(vndResource.getModel());

    return Single.just(response);
  }

  private void addProperties(HalResource vndResource, Throwable error) {

    vndResource.getModel().put("message", error.getMessage());
    vndResource.getModel().put("title", error.getClass().getName() + ": " + error.getMessage());
  }

  private void addEmbeddedCauses(HalResource vndResource, Throwable error) {
    Throwable cause = error.getCause();
    if (cause != null) {
      HalResource embedded = new HalResource();
      addProperties(embedded, cause);
      addEmbeddedCauses(embedded, cause);

      vndResource.addEmbedded("errors", embedded);
    }
  }


  private void addAboutLink(HalResource vndResource, LinkableResource resourceImpl) {
    Link clonedLink = null;
    try {
      Link link = resourceImpl.createLink();
      clonedLink = new Link(link.getModel().deepCopy());
    }
    catch (RuntimeException ex) {
      //
    }

    if (clonedLink != null) {
      vndResource.addLinks("about", clonedLink);
    }
  }

}
