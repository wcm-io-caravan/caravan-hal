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
package io.wcm.caravan.hal.comparison.impl.util;

import java.util.Collection;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.hal.resource.HalObject;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Utility methods to create JSON representations from {@link HalResource}, {@link Link} and Collections
 */
public final class HalStringConversion {

  private HalStringConversion() {
    // static utility methods only
  }

  /**
   * @param resourcesOrLinks to format
   * @return a JSON string
   */
  public static String asString(Collection<? extends HalObject> resourcesOrLinks) {
    return resourcesOrLinks.stream()
        .map(HalStringConversion::asString)
        .collect(Collectors.joining(",\n", "[\n", "\n]"));
  }

  /**
   * @param resourcesOrLinks to format
   * @return a JSON string
   */
  public static String asString(HalObject resourcesOrLinks) {
    return asString(resourcesOrLinks.getModel());
  }

  /**
   * @param node to format
   * @return a JSON string
   */
  public static String asString(JsonNode node) {
    return node.toString();
  }
}
