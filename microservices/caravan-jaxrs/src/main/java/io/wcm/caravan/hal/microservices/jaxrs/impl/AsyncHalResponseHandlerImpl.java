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
import javax.ws.rs.core.UriInfo;

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
  public void respondWith(LinkableResource resourceImpl, UriInfo uriInfo, AsyncResponse suspended, RequestMetricsCollector metrics) {

    // create a response renderer with a strategy that is able to extract the status code
    // from any JAX-RS WebApplicationException that might be thrown in the resource implementations
    AsyncHalResponseRenderer renderer = AsyncHalResponseRenderer.create(metrics, exceptionStrategy);

    // asynchronously render the given resource (or create a vnd.error response if any exceptions are thrown)
    String requestUri = uriInfo.getRequestUri().toString();
    Single<HalResponse> rxResponse = renderer.renderResponse(requestUri, resourceImpl);

    rxResponse.subscribe(
        // return the HAL or VND+Error response when it is available
        halResponse -> resumeWithResponse(suspended, halResponse),
        // or fall back to the regular JAX-RS error handling if an exception was not caught
        fatalException -> resumeWithError(uriInfo, suspended, fatalException));
  }

  private void resumeWithResponse(AsyncResponse suspended, HalResponse halResponse) {

    // build a JAX-RS response from the HAL response created by the AsyncHalResponseRenderer
    ResponseBuilder jaxRsResponse = Response
        .status(halResponse.getStatus())
        .type(halResponse.getContentType())
        .entity(halResponse.getBody());

    // add a CacheControl header only if a max-age value should be set
    Integer maxAge = halResponse.getMaxAge();
    if (maxAge != null) {
      CacheControl cacheControl = new CacheControl();
      cacheControl.setMaxAge(maxAge);
      jaxRsResponse.cacheControl(cacheControl);
    }

    // send the response to the client (HalResourceMessageBodyWriter will be responsible to serialise the body)
    suspended.resume(jaxRsResponse.build());
  }

  private void resumeWithError(UriInfo uriInfo, AsyncResponse suspended, Throwable fatalError) {
    log.error("A fatal exception occured when handling request for " + uriInfo.getRequestUri(), fatalError);

    suspended.resume(fatalError);
  }

  /**
   * This strategy allows server-side resource implementations to throw any subclass of {@link WebApplicationException},
   * and ensure that the correct status code is actually added to the {@link HalResponse} instance by the
   * {@link AsyncHalResponseRenderer}
   */
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
