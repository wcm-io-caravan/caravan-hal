/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
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
package io.wcm.caravan.hal.resource;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Factory for HAL {@link HalResource}s.
 * @deprecated just create {@link HalResource} and {@link Link} instances using the new constructors
 */
@Deprecated
@ProviderType
public final class HalResourceFactory {

  /**
   * JSON object mapper
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private HalResourceFactory() {
    // nothing to do
  }

  /**
   * Converts any object into a JSON {@link ObjectNode}.
   * @param input Any object
   * @return JSON object node
   */
  public static ObjectNode convert(Object input) {
    return OBJECT_MAPPER.convertValue(input, ObjectNode.class);
  }

  /**
   * Creates a HAL link with the given HREF.
   * @param href Link HREF
   * @return Link
   * @deprecated use the constructor {@link Link#Link(String) instead}
   */
  @Deprecated
  public static Link createLink(String href) {
    return new Link(href);
  }

  /**
   * Creates a HAL resource with empty state but a self link. Mostly needed for index resources.
   * @param href The self HREF for the resource
   * @return New HAL resource
   * @deprecated just create {@link HalResource} and {@link Link} instances using the new constructors
   */
  @Deprecated
  public static HalResource createResource(String href) {
    return new HalResource(href);
  }

  /**
   * Creates a HAL resource with state and a self link.
   * @param model The state of the resource
   * @param href The self link for the resource
   * @return New HAL resource
   * @deprecated just create {@link HalResource} and {@link Link} instances using the new constructors
   */
  @Deprecated
  public static HalResource createResource(Object model, String href) {
    return new HalResource(convert(model), href);
  }

  /**
   * Creates a HAL resource with state and a self link.
   * @param model The state of the resource
   * @param href The self link for the resource
   * @return New HAL resource
   * @deprecated just create {@link HalResource} and {@link Link} instances using the new constructors
   */
  @Deprecated
  public static HalResource createResource(ObjectNode model, String href) {
    return new HalResource(model, href);
  }

  /**
   * Converts the JSON model to an object of the given type.
   * @param halResource HAL resource with model to convert
   * @param type Type of the requested object
   * @param <T> Output type
   * @return State as object
   */
  public static <T> T getStateAsObject(HalResource halResource, Class<T> type) {
    return HalResourceFactory.OBJECT_MAPPER.convertValue(halResource.getModel(), type);
  }

}
