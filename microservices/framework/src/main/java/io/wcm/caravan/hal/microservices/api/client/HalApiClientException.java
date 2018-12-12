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

  private final HalResponse errorResponse;
  private final String requestUrl;

  public HalApiClientException(HalApiMethodInvocation invocation, HalApiClientException cause) {
    super("Failed to load an upstream resource that was requested by calling " + invocation, cause);
    this.errorResponse = cause.getErrorResponse();
    this.requestUrl = cause.getRequestUrl();
  }

  public HalApiClientException(String message, Integer statusCode, String requestUrl) {
    this(message, statusCode, requestUrl, null);
  }

  public HalApiClientException(String message, Integer statusCode, String requestUrl, Throwable cause) {
    super(message, cause);
    this.errorResponse = new HalResponse().withStatus(statusCode);
    this.requestUrl = requestUrl;
  }

  public HalApiClientException(HalResponse errorResponse, String requestUrl, Throwable cause) {
    super("HTTP request failed with status code " + errorResponse.getStatus(), cause);
    this.errorResponse = errorResponse;
    this.requestUrl = requestUrl;
  }

  public HalResponse getErrorResponse() {
    return errorResponse;
  }

  public Integer getStatusCode() {
    return errorResponse.getStatus();
  }

  public String getRequestUrl() {
    return requestUrl;
  }

}
