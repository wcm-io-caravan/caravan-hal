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
import static io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResponseRendererImpl.CARAVAN_METADATA_RELATION;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class VndErrorResponseRendererImpl implements VndErrorResponseRenderer {

  private static final Logger log = LoggerFactory.getLogger(VndErrorResponseRendererImpl.class);

  static final String CONTENT_TYPE = "application/vnd.error+json";

  private final ExceptionStatusAndLoggingStrategy statusCodeExtractor;

  public VndErrorResponseRendererImpl(ExceptionStatusAndLoggingStrategy statusCodeExtractor) {
    this.statusCodeExtractor = statusCodeExtractor;
  }

  @Override
  public HalResponse renderError(LinkableResource resourceImpl, Throwable error, RequestMetricsCollector metrics) {

    HalResource vndResource = new HalResource();

    addProperties(vndResource, error);
    addEmbeddedCauses(vndResource, error);
    AsyncHalResponseRendererImpl.addMetadata(metrics, vndResource, resourceImpl);

    String uri = addAboutLinkAndReturnResourceUri(vndResource, resourceImpl);
    int status = ObjectUtils.defaultIfNull(statusCodeExtractor.extractStatusCode(error), 500);
    logError(error, uri, status);

    return new HalResponse()
        .withStatus(status)
        .withContentType(CONTENT_TYPE)
        .withBody(vndResource);
  }

  private void logError(Throwable error, String uri, int status) {

    if (statusCodeExtractor.logAsCompactWarning(error)) {
      // if this error was caused by an upstream request, there is no need to include the full stack traces
      String messages = Stream.of(ExceptionUtils.getThrowables(error))
          .map(t -> t.getClass().getSimpleName() + ": " + t.getMessage())
          .collect(Collectors.joining("\n"));

      log.warn("Responding with " + status + " for " + uri + ":\n" + messages);
    }
    else {
      log.error("Responding with " + status + " for " + uri, error);
    }
  }

  private void addProperties(HalResource vndResource, Throwable error) {

    vndResource.getModel().put("message", error.getMessage());
    vndResource.getModel().put("title", error.getClass().getName() + ": " + error.getMessage());
  }

  private String addAboutLinkAndReturnResourceUri(HalResource vndResource, LinkableResource resourceImpl) {
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

    return clonedLink != null ? clonedLink.getHref() : "(unknown URI)";
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
