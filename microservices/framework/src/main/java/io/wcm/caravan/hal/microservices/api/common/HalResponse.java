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
  private final String contentType;
  private final String reason;
  private final HalResource body;
  private final Integer maxAge;

  public HalResponse() {
    this.status = null;
    this.contentType = null;
    this.reason = null;
    this.body = null;
    this.maxAge = null;
  }

  private HalResponse(Integer status, String contentType, String reason, HalResource body, Integer maxAge) {
    this.status = status;
    this.contentType = contentType;
    this.reason = reason;
    this.body = body;
    this.maxAge = maxAge;
  }

  public Integer getStatus() {
    return status;
  }

  public HalResponse withStatus(Integer value) {
    return new HalResponse(value, contentType, reason, body, maxAge);
  }

  public String getContentType() {
    return contentType;
  }

  public HalResponse withContentType(String value) {
    return new HalResponse(status, value, reason, body, maxAge);
  }

  public String getReason() {
    return reason;
  }

  public HalResponse withReason(String value) {
    return new HalResponse(status, contentType, value, body, maxAge);
  }

  public HalResource getBody() {
    return body;
  }

  public HalResponse withBody(HalResource value) {
    return new HalResponse(status, contentType, reason, value, maxAge);
  }

  public HalResponse withBody(JsonNode value) {
    HalResource hal = value != null ? new HalResource(value) : null;
    return new HalResponse(status, contentType, reason, hal, maxAge);
  }

  public Integer getMaxAge() {
    return maxAge;
  }

  public HalResponse withMaxAge(Integer value) {
    return new HalResponse(status, contentType, reason, body, value);
  }
}
