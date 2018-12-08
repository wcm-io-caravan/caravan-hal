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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResponse;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;


class CaravanGuavaJsonResourceLoader implements JsonResourceLoader {

  private static final JsonFactory JSON_FACTORY = new JsonFactory(new ObjectMapper());

  private static final Cache<String, JsonResponse> SHARED_CACHE = CacheBuilder.newBuilder().build();

  private final String serviceId;

  private final CaravanHttpClient client;

  CaravanGuavaJsonResourceLoader(CaravanHttpClient client, String serviceId) {
    this.serviceId = serviceId;
    this.client = client;
  }

  @Override
  public Single<JsonResponse> loadJsonResource(String uri) {

    JsonResponse cached = SHARED_CACHE.getIfPresent(uri);
    if (cached != null) {
      return Single.just(cached);
    }

    CaravanHttpRequest request = createRequest(uri);

    return executeRequest(request)
        .map(response -> parseResponse(uri, response));
  }

  private CaravanHttpRequest createRequest(String uri) {

    CaravanHttpRequestBuilder requestBuilder = new CaravanHttpRequestBuilder(serviceId);

    requestBuilder.append(uri);

    return requestBuilder.build();
  }

  private Single<CaravanHttpResponse> executeRequest(CaravanHttpRequest request) {

    return RxJavaInterop.toV2Single(client.execute(request).toSingle());
  }

  private JsonResponse parseResponse(String uri, CaravanHttpResponse response) {
    try {

      int statusCode = response.status();
      JsonNode jsonNode;
      Integer maxAge = null;
      if (statusCode >= 400) {
        jsonNode = null;
        maxAge = 60;
      }
      else {
        jsonNode = JSON_FACTORY.createParser(response.body().asString()).readValueAsTree();
        String maxAgeString = response.getCacheControl().get("max-age");
        if (maxAgeString != null) {
          try {
            maxAge = Integer.parseInt(maxAgeString);
          }
          catch (NumberFormatException ex) {
            // ignore
          }
        }
      }

      JsonResponse jsonResponse = new JsonResponse()
          .withStatus(statusCode)
          .withReason(response.reason())
          .withBody(jsonNode)
          .withMaxAge(maxAge);

      SHARED_CACHE.put(uri, jsonResponse);

      return jsonResponse;

    }
    catch (JsonParseException ex) {
      throw new RuntimeException("Failed to parse HAL/JSON from " + uri, ex);
    }
    catch (IOException ex) {
      throw new RuntimeException("Failed to transfer HAL/JSON from " + uri, ex);
    }
    // CHECKSTYLE:OFF - yes, we want to catch and rethrow all runtime exceptions
    catch (RuntimeException ex) {
      throw new RuntimeException("Failed to process HAL/JSON response from " + uri, ex);
    }
  }


}
