/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.hal.microservices.jaxrs;

import java.util.Objects;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.LinkBuilder;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.caravan.CaravanHalApiClient;
import io.wcm.caravan.hal.microservices.jaxrs.impl.JaxRsLinkBuilderFactory;

public class CaravanJaxRsHalOrchestratorImpl implements HalOrchestrator {

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

  @Context
  private UriInfo uriInfo;

  @Context
  private AsyncResponse response;

  @Context
  private AsyncHalResponseHandler responseHandler;

  @Context
  private UpstreamServiceRegistry registry;

  @Context
  private CaravanHalApiClient halApiClient;

  @Context
  private JaxRsBundleInfo bundleInfo;

  @Override
  public void limitOutputMaxAge(int maxAge) {
    metrics.limitOutputMaxAge(maxAge);
  }

  @Override
  public <T> Single<T> getEntryPoint(Class<T> halApiInterface) {

    return registry.getUpstreamServiceIds(uriInfo)
        .map(serviceIdMap -> serviceIdMap.get(halApiInterface))
        .filter(Objects::nonNull)
        .switchIfEmpty(Single.error(new RuntimeException("No serviceId availbale for HAL API interface" + halApiInterface.getName())))
        .flatMap(serviceId -> registry.getEntryPointUri(serviceId, uriInfo)
            .map(uri -> halApiClient.getEntryPoint(serviceId, uri, halApiInterface, metrics)));
  }


  @Override
  public LinkBuilder createLinkBuilder() {
    return JaxRsLinkBuilderFactory.createLinkBuilder(bundleInfo.getApplicationPath());
  }

  @Override
  public void respondWith(LinkableResource resourceImpl) {
    responseHandler.respondWith(resourceImpl, uriInfo, response, metrics);
  }

}
