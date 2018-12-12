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
package io.wcm.caravan.hal.microservices.caravan.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.caravan.CaravanHalApiClient;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.pipeline.JsonPipelineFactory;

@Component
public class CaravanHalApiClientImpl implements CaravanHalApiClient {

  @Reference
  private CaravanHttpClient httpClient;

  @Reference
  private JsonPipelineFactory pipelineFactory;

  @Override
  public <T> T getEntryPoint(String serviceId, String uri, Class<T> halApiInterface, RequestMetricsCollector metrics) {

    //JsonResourceLoader jsonLoader = new CaravanJsonPipelineResourceLoader(pipelineFactory, serviceId);
    JsonResourceLoader jsonLoader = new CaravanGuavaJsonResourceLoader(httpClient, serviceId);
    CaravanBinaryResourceLoader binaryLoader = new CaravanBinaryResourceLoader(httpClient, serviceId);

    HalApiClient client = HalApiClient.create(jsonLoader, binaryLoader, metrics);

    return client.getEntryPoint(uri, halApiInterface);
  }

}
