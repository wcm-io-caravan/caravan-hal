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
package io.wcm.caravan.hal.api.client.impl.resourceloader.caravan;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;

import io.wcm.caravan.hal.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.api.client.RequestMetricsCollector;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import rx.Observable;


public class CaravanJsonPipelineResourceLoader implements JsonResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(CaravanJsonPipelineResourceLoader.class);

  private final JsonPipelineFactory pipelineFactory;
  private final String serviceId;

  CaravanJsonPipelineResourceLoader(JsonPipelineFactory pipelineFactory, String serviceId) {
    this.pipelineFactory = pipelineFactory;
    this.serviceId = serviceId;
  }

  @Override
  public Observable<JsonNode> loadJsonResource(String uri, RequestMetricsCollector metrics) {

    CaravanHttpRequest request = createRequest(uri);

    JsonPipeline pipeline = pipelineFactory.create(request);

    Stopwatch stopwatch = Stopwatch.createStarted();

    return pipeline.getOutput()
        .map(pipelineOutput -> {

          log.debug("Received JSON response from {} with status code {} and max-age {}", uri, pipelineOutput.getStatusCode(), pipelineOutput.getMaxAge());

          JsonNode payload = pipelineOutput.getPayload();

          String title = getResourceTitle(payload);

          metrics.onResponseRetrieved(uri, title, pipelineOutput.getMaxAge(), stopwatch.elapsed(TimeUnit.MICROSECONDS));

          return payload;
        });
  }

  private String getResourceTitle(JsonNode payload) {

    HalResource halResource = new HalResource(payload);

    Link selfLink = halResource.getLink();
    String title = null;
    if (selfLink != null) {
      title = selfLink.getTitle();
    }

    if (title == null) {
      title = "Untitled HAL resource";
      if (serviceId != null) {
        title += " from service " + serviceId;
      }
    }

    return title;
  }

  private CaravanHttpRequest createRequest(String uri) {
    CaravanHttpRequestBuilder requestBuilder = new CaravanHttpRequestBuilder(serviceId);

    requestBuilder.append(uri);

    return requestBuilder.build();
  }
}
