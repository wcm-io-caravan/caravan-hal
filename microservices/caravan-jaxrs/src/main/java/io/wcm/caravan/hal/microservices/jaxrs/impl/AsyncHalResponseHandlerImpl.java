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

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.jaxrs.AsyncHalResponseHandler;
import io.wcm.caravan.hal.resource.HalResource;

@Component(service = { AsyncHalResponseHandler.class })
public class AsyncHalResponseHandlerImpl implements AsyncHalResponseHandler {

  private static final Logger log = LoggerFactory.getLogger(AsyncHalResponseHandlerImpl.class);

  @Override
  public void respondWith(LinkableResource resourceImpl, AsyncResponse asyncResponse, RequestMetricsCollector metrics) {

    AsyncHalResourceRenderer renderer = AsyncHalResourceRenderer.create(metrics);

    Single<HalResource> rxHalResource = renderer.renderResource(resourceImpl);

    rxHalResource.subscribe(new SingleObserver<HalResource>() {

      @Override
      public void onSubscribe(Disposable d) {
        // nothing to do here
      }

      @Override
      public void onSuccess(HalResource value) {

        HalResource metadata = metrics.createMetadataResource(resourceImpl);

        value.addEmbedded("caravan:metadata", metadata);

        ResponseBuilder response = Response.ok(value);

        Integer maxAge = metrics.getOutputMaxAge();
        if (maxAge != null) {
          CacheControl cacheControl = new CacheControl();
          cacheControl.setMaxAge(maxAge);
          response.cacheControl(cacheControl);
        }

        asyncResponse.resume(response.build());
      }

      @Override
      public void onError(Throwable error) {

        log.error("Failed to handle request", error);

        String stackTrace = ExceptionUtils.getStackTrace(error);
        asyncResponse.resume(Response.serverError().entity(stackTrace).build());
      }

    });
  }

}
