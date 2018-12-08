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

import com.fasterxml.jackson.databind.JsonNode;

public class JsonResponse {

  private final Integer status;
  private final String reason;
  private final JsonNode body;
  private final Integer maxAge;

  public JsonResponse(Integer status, String reason, JsonNode body, Integer maxAge) {
    this.status = status;
    this.reason = reason;
    this.body = body;
    this.maxAge = maxAge;
  }

  public JsonResponse() {
    this.status = null;
    this.reason = null;
    this.body = null;
    this.maxAge = null;
  }

  public Integer getStatus() {
    return this.status;
  }

  public JsonResponse withStatus(Integer value) {
    return new JsonResponse(value, reason, body, maxAge);
  }

  public String getReason() {
    return this.reason;
  }

  public JsonResponse withReason(String value) {
    return new JsonResponse(status, value, body, maxAge);
  }

  public JsonNode getBody() {
    return this.body;
  }

  public JsonResponse withBody(JsonNode value) {
    return new JsonResponse(status, reason, value, maxAge);
  }

  public Integer getMaxAge() {
    return this.maxAge;
  }

  public JsonResponse withMaxAge(Integer value) {
    return new JsonResponse(status, reason, body, value);
  }

}
