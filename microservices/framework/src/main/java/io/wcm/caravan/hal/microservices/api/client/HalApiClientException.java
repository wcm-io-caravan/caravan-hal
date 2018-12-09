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
package io.wcm.caravan.hal.microservices.api.client;

import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.impl.client.HalApiMethodInvocation;

public class HalApiClientException extends RuntimeException {

  private final HalResponse failedResponse;
  private final String requestUrl;

  public HalApiClientException(HalApiMethodInvocation invocation, HalApiClientException cause) {
    super("An upstream resource required to resolve " + invocation + " failed to load", cause);
    this.failedResponse = cause.getFailedResponse();
    this.requestUrl = cause.getRequestUrl();
  }

  public HalApiClientException(String message, int statusCode, String requestUrl) {
    this(new HalResponse().withStatus(statusCode).withReason(message), requestUrl);
  }

  public HalApiClientException(HalResponse failedResponse, String requestUrl) {
    super("HTTP request for " + requestUrl + " failed with status code " + failedResponse.getStatus(), failedResponse.getCause());
    this.failedResponse = failedResponse;
    this.requestUrl = requestUrl;
  }

  public HalResponse getFailedResponse() {
    return failedResponse;
  }

  public int getStatusCode() {
    return failedResponse.getStatus();
  }

  public String getRequestUrl() {
    return requestUrl;
  }

}
