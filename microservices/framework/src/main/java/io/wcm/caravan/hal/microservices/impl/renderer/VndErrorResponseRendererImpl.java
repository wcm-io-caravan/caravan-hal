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

import static io.wcm.caravan.hal.api.relations.StandardRelations.VIA;
import static io.wcm.caravan.hal.microservices.api.common.VndErrorRelations.ABOUT;
import static io.wcm.caravan.hal.microservices.api.common.VndErrorRelations.ERRORS;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.api.relations.StandardRelations;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.hal.microservices.impl.metadata.ResponseMetadataRelations;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Full implementation of {@link VndErrorResponseRenderer}
 */
public class VndErrorResponseRendererImpl implements VndErrorResponseRenderer {

  private static final Logger log = LoggerFactory.getLogger(VndErrorResponseRendererImpl.class);

  private static final DefaultExceptionStatusAndLoggingStrategy DEFAULT_STRATEGY = new DefaultExceptionStatusAndLoggingStrategy();

  private final ExceptionStatusAndLoggingStrategy strategy;

  /**
   * @param customStrategy allows to control the status code and logging of exceptions
   */
  public VndErrorResponseRendererImpl(ExceptionStatusAndLoggingStrategy customStrategy) {
    this.strategy = DEFAULT_STRATEGY.decorateWith(customStrategy);
  }

  @Override
  public HalResponse renderError(String requestUri, LinkableResource resourceImpl, Throwable error, RequestMetricsCollector metrics) {

    HalResource vndResource = new HalResource();

    addProperties(vndResource, error);
    addAboutLinkAndReturnResourceUri(vndResource, resourceImpl, requestUri);

    addEmbeddedCauses(vndResource, error);
    AsyncHalResponseRendererImpl.addMetadata(metrics, vndResource, resourceImpl);

    int status = ObjectUtils.defaultIfNull(strategy.extractStatusCode(error), 500);
    logError(error, requestUri, status);

    return new HalResponse()
        .withStatus(status)
        .withContentType(VndErrorResponseRenderer.CONTENT_TYPE)
        .withBody(vndResource);
  }

  private void logError(Throwable error, String uri, int status) {

    if (strategy.logAsCompactWarning(error)) {
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
    vndResource.getModel().put("class", error.getClass().getName());
    vndResource.getModel().put("title", error.getClass().getSimpleName() + ": " + error.getMessage());
  }

  private void addAboutLinkAndReturnResourceUri(HalResource vndResource, LinkableResource resourceImpl, String requestUri) {

    Link aboutLink = new Link(requestUri).setTitle("The URI of this resource as it was actually requested");
    vndResource.addLinks(ABOUT, aboutLink);

    Link selfLink = null;
    try {
      selfLink = resourceImpl.createLink()
          .setTitle("The URI as reported by the self-link of this resource");
    }
    // CHECKSTYLE:OFF - we really want to ignore any exceptions that could be thrown when creating the self-link
    catch (RuntimeException ex) {
      // CHECKSTYLE:ON
    }

    if (selfLink != null && !requestUri.endsWith(selfLink.getHref())) {
      vndResource.addLinks(StandardRelations.CANONICAL, selfLink);
    }

  }

  private void addEmbeddedCauses(HalResource vndResource, Throwable error) {
    Throwable cause = error.getCause();
    if (cause != null) {
      HalResource embedded = new HalResource();
      addProperties(embedded, cause);

      List<HalResource> vndErrorsFoundInBody = getErrorsFromUpstreamResponse(vndResource, cause);

      vndResource.addEmbedded(ERRORS, embedded);

      addEmbeddedCauses(vndResource, cause);

      if (vndErrorsFoundInBody != null) {
        vndResource.addEmbedded(ERRORS, vndErrorsFoundInBody);
      }

    }
  }

  private List<HalResource> getErrorsFromUpstreamResponse(HalResource context, Throwable cause) {

    if (cause instanceof HalApiClientException) {
      HalApiClientException hace = (HalApiClientException)cause;

      Link link = new Link(hace.getRequestUrl()).setTitle("The upstream resource that could not be loaded");

      context.addLinks(VIA, link);

      HalResponse upstreamJson = hace.getErrorResponse();
      HalResource upstreamBody = upstreamJson.getBody();

      if (upstreamBody != null && upstreamBody.getModel().size() > 0) {
        HalResource causeFromBody = new HalResource(upstreamBody.getModel().deepCopy());
        causeFromBody.removeEmbedded(ResponseMetadataRelations.CARAVAN_METADATA_RELATION);

        List<HalResource> embeddedCauses = causeFromBody.getEmbedded(ERRORS);

        List<HalResource> flatCauses = new ArrayList<>();
        flatCauses.add(causeFromBody);
        flatCauses.addAll(embeddedCauses);

        causeFromBody.removeEmbedded(ERRORS);
        return flatCauses;
      }
    }

    return null;
  }


  private static final class DefaultExceptionStatusAndLoggingStrategy implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {

      if (error instanceof HalApiClientException) {
        return ((HalApiClientException)error).getStatusCode();
      }

      return null;
    }

    @Override
    public boolean logAsCompactWarning(Throwable error) {

      if (error instanceof HalApiClientException) {
        return true;
      }

      return false;
    }

    public ExceptionStatusAndLoggingStrategy decorateWith(ExceptionStatusAndLoggingStrategy customStrategy) {

      if (customStrategy == null) {
        return this;
      }

      return new MultiExceptionStatusAndLoggingStrategy(ImmutableList.of(customStrategy, this));
    }
  }

  private static final class MultiExceptionStatusAndLoggingStrategy implements ExceptionStatusAndLoggingStrategy {

    private final List<ExceptionStatusAndLoggingStrategy> strategies;

    MultiExceptionStatusAndLoggingStrategy(List<ExceptionStatusAndLoggingStrategy> strategies) {
      this.strategies = strategies;
    }

    @Override
    public Integer extractStatusCode(Throwable error) {

      return strategies.stream()
          .map(s -> s.extractStatusCode(error))
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
    }

    @Override
    public boolean logAsCompactWarning(Throwable error) {

      return strategies.stream()
          .map(s -> s.logAsCompactWarning(error))
          .reduce(false, (logAsWarning1, logAsWarning2) -> logAsWarning1 || logAsWarning2);
    }
  }
}
