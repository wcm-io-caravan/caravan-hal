/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.hal.microservices.jaxrs.impl;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.jaxrs.AsyncHalResponseHandler;

@Component(service = { AsyncHalResponseHandler.class })
public class AsyncHalResponseHandlerImpl implements AsyncHalResponseHandler {

  private static final Logger log = LoggerFactory.getLogger(AsyncHalResponseHandlerImpl.class);

  private final JaxRsExceptionStrategy exceptionStrategy = new JaxRsExceptionStrategy();

  @Override
  public void respondWith(LinkableResource resourceImpl, AsyncResponse suspended, RequestMetricsCollector metrics) {

    // create a response renderer with a strategy that is able to extract the status code
    // from any JAX-RS WebApplicationException that might be thrown in the resource implementations
    AsyncHalResponseRenderer renderer = AsyncHalResponseRenderer.create(metrics, exceptionStrategy);

    // asynchronously render the given resource (or create a vnd.error response if any exceptions are thrown)
    Single<HalResponse> rxResponse = renderer.renderResponse(resourceImpl);

    rxResponse.subscribe(
        // return the HAL or VND+Error response when it is available
        halResponse -> resumeWithResponse(suspended, halResponse),
        // or fall back to the regular JAX-RS error handling if an exception was not caught
        fatalException -> resumeWithError(suspended, fatalException));
  }

  private void resumeWithResponse(AsyncResponse suspended, HalResponse halResponse) {

    ResponseBuilder jaxRsResponse = Response
        .status(halResponse.getStatus())
        .type(halResponse.getContentType())
        .entity(halResponse.getBody());

    Integer maxAge = halResponse.getMaxAge();
    if (maxAge != null) {
      CacheControl cacheControl = new CacheControl();
      cacheControl.setMaxAge(maxAge);
      jaxRsResponse.cacheControl(cacheControl);
    }

    suspended.resume(jaxRsResponse.build());
  }

  private void resumeWithError(AsyncResponse suspended, Throwable error) {
    log.error("Failed to handle request", error);

    suspended.resume(error);
  }

  private static class JaxRsExceptionStrategy implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {

      if (error instanceof WebApplicationException) {
        return ((WebApplicationException)error).getResponse().getStatus();
      }

      return null;
    }
  }
}
