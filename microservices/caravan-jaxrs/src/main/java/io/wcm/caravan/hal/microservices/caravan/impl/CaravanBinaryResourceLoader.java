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
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;


public class CaravanBinaryResourceLoader implements BinaryResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(CaravanBinaryResourceLoader.class);

  private final CaravanHttpClient httpClient;
  private final String serviceId;

  CaravanBinaryResourceLoader(CaravanHttpClient httpClient, String serviceId) {
    this.httpClient = httpClient;
    this.serviceId = serviceId;
  }

  @Override
  public Single<InputStream> loadBinaryResource(String uri, RequestMetricsCollector metrics) {

    CaravanHttpRequest request = createRequest(uri);

    Single<CaravanHttpResponse> rxResponse = executeRequest(request);

    return rxResponse.flatMap(response -> getResponseAsInputStream(uri, request, response));
  }

  private CaravanHttpRequest createRequest(String uri) {
    CaravanHttpRequestBuilder requestBuilder = new CaravanHttpRequestBuilder(serviceId);

    requestBuilder.append(uri);

    return requestBuilder.build();
  }

  private Single<CaravanHttpResponse> executeRequest(CaravanHttpRequest request) {
    return RxJavaInterop.toV2Single(httpClient.execute(request).toSingle());
  }

  private SingleSource<? extends InputStream> getResponseAsInputStream(String uri, CaravanHttpRequest request, CaravanHttpResponse response) {
    log.debug("Received binary response from {} with status code {} and reason {}", uri, response.status(), response.reason());

    if (response.status() >= 400) {

      String bodyAsString = "";
      if (response.body() != null) {
        try {
          bodyAsString = response.body().asString();
        }
        catch (IOException ex) {
          return Single.error(new RequestFailedRuntimeException(request, "Failed to read response body", ex));
        }
      }

      return Single.error(new IllegalResponseRuntimeException(request, uri, response.status(), bodyAsString, response.reason()));
    }

    if (response.body() == null) {
      return Single.error(new IllegalResponseRuntimeException(request, uri, response.status(), "", "No response body was retrieved"));
    }

    try {
      return Single.just(response.body().asInputStream());
    }
    catch (IOException ex) {
      return Single.error(new RequestFailedRuntimeException(request, "Failed to read response body", ex));
    }
  }
}
