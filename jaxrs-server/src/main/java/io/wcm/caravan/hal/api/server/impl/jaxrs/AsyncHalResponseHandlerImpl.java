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
package io.wcm.caravan.hal.api.server.impl.jaxrs;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.hal.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.api.server.LinkableResource;
import io.wcm.caravan.hal.api.server.jaxrs.AsyncHalResponseHandler;
import io.wcm.caravan.hal.resource.HalResource;
import rx.Single;
import rx.SingleSubscriber;

@Component(service = { AsyncHalResponseHandler.class })
public class AsyncHalResponseHandlerImpl implements AsyncHalResponseHandler {

  private static final Logger log = LoggerFactory.getLogger(AsyncHalResponseHandlerImpl.class);

  @Reference
  private AsyncHalResourceRenderer renderer;

  @Override
  public void respondWith(LinkableResource resourceImpl, AsyncResponse asyncResponse) {

    Single<HalResource> rxHalResource = renderer.renderResource(resourceImpl);

    rxHalResource.subscribe(new SingleSubscriber<HalResource>() {

      @Override
      public void onSuccess(HalResource value) {
        asyncResponse.resume(value);
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
