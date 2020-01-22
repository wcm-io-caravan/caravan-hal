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
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;


class CaravanGuavaJsonResourceLoader implements JsonResourceLoader {

  private static final JsonFactory JSON_FACTORY = new JsonFactory(new ObjectMapper());

  private static final Cache<String, HalResponse> SHARED_CACHE = CacheBuilder.newBuilder().build();

  private final String serviceId;

  private final CaravanHttpClient client;

  CaravanGuavaJsonResourceLoader(CaravanHttpClient client, String serviceId) {
    this.serviceId = serviceId;
    this.client = client;
  }

  @Override
  public Single<HalResponse> loadJsonResource(String uri) {

    HalResponse cached = SHARED_CACHE.getIfPresent(uri);

    // TODO: ignore stale cache entries (based on their original maxAge value)
    if (cached != null) {
      return Single.just(cached);
    }

    CaravanHttpRequest request = createRequest(uri);

    return executeRequest(request)
        .map(response -> parseResponse(uri, request, response))
        .onErrorResumeNext(ex -> rethrowAsHalApiClientException(ex, uri));
  }

  private CaravanHttpRequest createRequest(String uri) {

    CaravanHttpRequestBuilder requestBuilder = new CaravanHttpRequestBuilder(serviceId);

    requestBuilder.append(uri);

    return requestBuilder.build();
  }

  private Single<CaravanHttpResponse> executeRequest(CaravanHttpRequest request) {

    return RxJavaInterop.toV2Single(client.execute(request).toSingle());
  }

  private HalResponse parseResponse(String uri, CaravanHttpRequest request, CaravanHttpResponse response) {
    try {

      int statusCode = response.status();
      String responseBody = response.body().asString();

      JsonNode jsonNode;
      Integer maxAge;
      if (statusCode >= 400) {
        jsonNode = parseResponseBodyAndIgnoreErrors(responseBody);
        maxAge = 60;
      }
      else {
        jsonNode = parseResponseBody(responseBody);
        maxAge = parseMaxAge(response);
      }

      HalResponse jsonResponse = new HalResponse()
          .withStatus(statusCode)
          .withReason(response.reason())
          .withBody(jsonNode)
          .withMaxAge(maxAge);

      if (statusCode >= 400) {
        IllegalResponseRuntimeException cause = new IllegalResponseRuntimeException(request, uri, statusCode, responseBody,
            "Received " + statusCode + " response from " + uri);
        throw new HalApiClientException(jsonResponse, uri, cause);
      }

      SHARED_CACHE.put(uri, jsonResponse);

      return jsonResponse;

    }
    catch (HalApiClientException ex) {
      throw ex;
    }
    catch (JsonParseException ex) {
      throw new RuntimeException("Failed to parse HAL/JSON body from " + uri, ex);
    }
    catch (IOException ex) {
      throw new RuntimeException("Failed to transfer HAL/JSON body from " + uri, ex);
    }
    // CHECKSTYLE:OFF - yes, we want to catch and rethrow all runtime exceptions
    catch (RuntimeException ex) {
      throw new RuntimeException("Failed to process HAL/JSON response body from " + uri, ex);
    }
  }

  private JsonNode parseResponseBody(String responseBody) throws IOException, JsonParseException {
    return JSON_FACTORY.createParser(responseBody).readValueAsTree();
  }

  private Integer parseMaxAge(CaravanHttpResponse response) {

    Integer maxAge = null;
    String maxAgeString = response.getCacheControl().get("max-age");
    if (maxAgeString != null) {
      try {
        maxAge = Integer.parseInt(maxAgeString);
      }
      catch (NumberFormatException ex) {
        // ignore
      }
    }
    return maxAge;
  }

  private JsonNode parseResponseBodyAndIgnoreErrors(String responseBody) {
    JsonNode jsonNode = null;
    try {
      jsonNode = parseResponseBody(responseBody);
    }
    catch (Exception ex) {
      // ignore any exceptions when trying to parse the response body for 40x errors
    }
    return jsonNode;
  }

  private Single<HalResponse> rethrowAsHalApiClientException(Throwable ex, String uri) {

    if (ex instanceof HalApiClientException) {
      return Single.error(ex);
    }

    if (ex instanceof IllegalResponseRuntimeException) {
      IllegalResponseRuntimeException irre = ((IllegalResponseRuntimeException)ex);

      JsonNode body = parseResponseBodyAndIgnoreErrors(irre.getResponseBody());

      HalResponse jsonResponse = new HalResponse()
          .withStatus(irre.getResponseStatusCode())
          .withBody(body);

      return Single.error(new HalApiClientException(jsonResponse, uri, ex));
    }

    String message = "HTTP request for " + uri + " failed because of timeout, configuration or networking issues";
    return Single.error(new HalApiClientException(message, 0, uri, ex));
  }


}