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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalObject;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Utility methods to create JSON representations from {@link HalResource}, {@link Link} and Collections
 */
public final class HalJsonConversion {

  private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

  private static final JsonNode MISSING_NODE = FACTORY.objectNode().path("missing");

  private HalJsonConversion() {
    // static utility methods only
  }

  /**
   * @param resourcesOrLinks
   * @return an {@link ArrayNode}
   */
  public static ArrayNode asJson(Iterable<? extends HalObject> resourcesOrLinks) {

    ArrayNode array = FACTORY.arrayNode();

    for (HalObject hal : resourcesOrLinks) {
      array.add(asJson(hal));
    }

    return array;
  }

  /**
   * @param resourceOrLink to format
   * @return an
   */
  public static JsonNode asJson(HalObject resourceOrLink) {
    if (resourceOrLink == null) {
      return MISSING_NODE;
    }
    return resourceOrLink.getModel();
  }

  /**
   * @param node to format
   * @return a JSON string
   */
  public static String asString(JsonNode node) {
    return node != null ? node.toString() : null;
  }

  public static ObjectNode cloneAndStripHalProperties(ObjectNode node) {
    ObjectNode clone = node.deepCopy();
    clone.remove("_links");
    clone.remove("_embedded");
    return clone;
  }
}
