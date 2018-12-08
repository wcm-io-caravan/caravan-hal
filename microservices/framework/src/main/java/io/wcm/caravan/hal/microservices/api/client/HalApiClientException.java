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


public class HalApiClientException extends RuntimeException {

  private final int statusCode;
  private final String requestUrl;

  public HalApiClientException(String message, int statusCode, String requestUrl, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
    this.requestUrl = requestUrl;
  }

  public HalApiClientException(String message, int statusCode, String requestUrl) {
    this(message, statusCode, requestUrl, null);
  }

  public HalApiClientException(JsonResponse failedResponse, String requestUrl) {
    this("HTTP request for " + requestUrl + " failed", failedResponse.getStatus(), requestUrl, failedResponse.getCause());
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public String getRequestUrl() {
    return this.requestUrl;
  }

}
