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
package io.wcm.caravan.hal.microservices.caravan.impl;

import java.util.concurrent.TimeUnit;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResponse;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategies;


class CaravanJsonPipelineResourceLoader implements JsonResourceLoader {

  private final JsonPipelineFactory pipelineFactory;
  private final String serviceId;

  CaravanJsonPipelineResourceLoader(JsonPipelineFactory pipelineFactory, String serviceId) {
    this.pipelineFactory = pipelineFactory;
    this.serviceId = serviceId;
  }

  @Override
  public Single<JsonResponse> loadJsonResource(String uri) {

    CaravanHttpRequest request = createRequest(uri);

    return getPipelineOutput(request)
        .map(pipelineOutput -> {

          JsonResponse response = new JsonResponse()
              .withStatus(pipelineOutput.getStatusCode())
              .withBody(pipelineOutput.getPayload())
              .withMaxAge(pipelineOutput.getMaxAge());

          return response;
        });
  }

  private CaravanHttpRequest createRequest(String uri) {

    CaravanHttpRequestBuilder requestBuilder = new CaravanHttpRequestBuilder(serviceId);

    requestBuilder.append(uri);

    return requestBuilder.build();
  }

  private Single<JsonPipelineOutput> getPipelineOutput(CaravanHttpRequest request) {

    JsonPipeline pipeline = pipelineFactory.create(request)
        .addCachePoint(CacheStrategies.timeToIdle(60, TimeUnit.SECONDS));

    return RxJavaInterop.toV2Single(pipeline.getOutput().toSingle());
  }
}
