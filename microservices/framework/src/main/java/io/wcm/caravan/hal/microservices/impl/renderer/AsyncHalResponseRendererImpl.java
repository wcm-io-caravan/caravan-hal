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

import static io.wcm.caravan.hal.microservices.api.common.VndErrorRelations.ABOUT;
import static io.wcm.caravan.hal.microservices.api.common.VndErrorRelations.ERRORS;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class AsyncHalResponseRendererImpl implements AsyncHalResponseRenderer {

  static final String CARAVAN_METADATA_RELATION = "caravan:metadata";

  private final AsyncHalResourceRenderer renderer;

  private final RequestMetricsCollector metrics;

  public AsyncHalResponseRendererImpl(AsyncHalResourceRenderer renderer, RequestMetricsCollector metrics) {
    this.renderer = renderer;
    this.metrics = metrics;
  }

  @Override
  public Single<HalResponse> renderResponse(LinkableResource resourceImpl) {

    return renderer.renderResource(resourceImpl)
        .flatMap(hal -> this.createResponse(resourceImpl, hal))
        .onErrorResumeNext(ex -> this.handleError(resourceImpl, ex));
  }

  Single<HalResponse> createResponse(LinkableResource resourceImpl, HalResource hal) {

    addMetadata(hal, resourceImpl);

    HalResponse response = new HalResponse()
        .withStatus(200)
        .withReason("Ok")
        .withBody(hal)
        .withMaxAge(metrics.getOutputMaxAge());

    return Single.just(response);
  }

  private void addMetadata(HalResource hal, LinkableResource resourceImpl) {

    HalResource metadata = metrics.createMetadataResource(resourceImpl);
    if (metadata != null) {
      hal.addEmbedded(CARAVAN_METADATA_RELATION, metadata);
    }
  }

  Single<HalResponse> handleError(LinkableResource resourceImpl, Throwable error) {

    HalResource vndResource = new HalResource();

    addProperties(vndResource, error);
    addAboutLink(vndResource, resourceImpl);
    addEmbeddedCauses(vndResource, error);
    addMetadata(vndResource, resourceImpl);

    HalResponse response = new HalResponse()
        .withStatus(500)
        .withBody(vndResource);

    return Single.just(response);
  }

  private void addProperties(HalResource vndResource, Throwable error) {

    vndResource.getModel().put("message", error.getMessage());
    vndResource.getModel().put("title", error.getClass().getName() + ": " + error.getMessage());
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
      vndResource.addLinks(ABOUT, clonedLink);
    }
  }

  private void addEmbeddedCauses(HalResource vndResource, Throwable error) {
    Throwable cause = error.getCause();
    if (cause != null) {
      HalResource embedded = new HalResource();
      addProperties(embedded, cause);

      boolean vndErrorsFoundInBody = embeddErrorsFromUpstreamResponse(embedded, cause);

      vndResource.addEmbedded(ERRORS, embedded);

      if (!vndErrorsFoundInBody) {
        addEmbeddedCauses(vndResource, cause);
      }
    }
  }

  private boolean embeddErrorsFromUpstreamResponse(HalResource embedded, Throwable cause) {
    boolean vndErrorsFoundInBody = false;
    if (cause instanceof HalApiClientException) {
      HalApiClientException hace = (HalApiClientException)cause;

      Link link = new Link(hace.getRequestUrl()).setTitle("The upstream resource that could not be loaded");
      embedded.addLinks(ABOUT, link);

      HalResponse upstreamJson = hace.getErrorResponse();
      if (upstreamJson.getBody() != null && upstreamJson.getBody().getModel().size() > 0) {
        HalResource causeFromBody = new HalResource(upstreamJson.getBody().getModel().deepCopy());
        causeFromBody.removeEmbedded(CARAVAN_METADATA_RELATION);
        embedded.addEmbedded(ERRORS, causeFromBody);
        vndErrorsFoundInBody = true;
      }
    }
    return vndErrorsFoundInBody;
  }
}
