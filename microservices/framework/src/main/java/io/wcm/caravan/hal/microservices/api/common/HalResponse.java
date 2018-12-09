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
package io.wcm.caravan.hal.microservices.api.common;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.hal.resource.HalResource;

public class HalResponse {

  private final Integer status;
  private final String reason;
  private final HalResource body;
  private final Integer maxAge;

  private final Throwable cause;

  public HalResponse() {
    this.status = null;
    this.reason = null;
    this.body = null;
    this.maxAge = null;
    this.cause = null;
  }

  private HalResponse(Integer status, String reason, HalResource body, Integer maxAge, Throwable cause) {
    this.status = status;
    this.reason = reason;
    this.body = body;
    this.maxAge = maxAge;
    this.cause = cause;
  }

  public Integer getStatus() {
    return status;
  }

  public HalResponse withStatus(Integer value) {
    return new HalResponse(value, reason, body, maxAge, cause);
  }

  public String getReason() {
    return reason;
  }

  public HalResponse withReason(String value) {
    return new HalResponse(status, value, body, maxAge, cause);
  }

  public HalResource getBody() {
    return body;
  }

  public HalResponse withBody(HalResource value) {
    return new HalResponse(status, reason, value, maxAge, cause);
  }

  public HalResponse withBody(JsonNode value) {
    return new HalResponse(status, reason, new HalResource(value), maxAge, cause);
  }

  public Integer getMaxAge() {
    return maxAge;
  }

  public HalResponse withMaxAge(Integer value) {
    return new HalResponse(status, reason, body, value, cause);
  }

  public Throwable getCause() {
    return cause;
  }

  public HalResponse withCause(Throwable value) {
    return new HalResponse(status, reason, body, maxAge, value);
  }
}
