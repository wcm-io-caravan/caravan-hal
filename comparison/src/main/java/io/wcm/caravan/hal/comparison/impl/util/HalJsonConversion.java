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
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalObject;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Utility methods to extract the JSON representations from {@link HalResource}, {@link Link} and Collections
 */
public final class HalJsonConversion {

  private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

  private static final JsonNode MISSING_NODE = FACTORY.objectNode().path("missing");

  private HalJsonConversion() {
    // static utility methods only
  }

  /**
   * @param resourcesOrLinks a collection of {@link Link} or {@link HalResource}
   * @return an {@link ArrayNode} that contains the JSON representation of those objects
   */
  public static ArrayNode asJson(Iterable<? extends HalObject> resourcesOrLinks) {

    ArrayNode array = FACTORY.arrayNode();

    for (HalObject hal : resourcesOrLinks) {
      array.add(asJson(hal));
    }

    return array;
  }

  /**
   * @param resourceOrLink a {@link Link} or {@link HalResource}
   * @return the corresponding {@link ObjectNode} (or a {@link MissingNode} for null input)
   */
  public static JsonNode asJson(HalObject resourceOrLink) {
    if (resourceOrLink == null) {
      return MISSING_NODE;
    }
    return resourceOrLink.getModel();
  }

  /**
   * @param hal a HalResource
   * @return a cloned {@link ObjectNode} that contains only the resource state (i.e. links and embedded resources are
   *         stripped)
   */
  public static ObjectNode cloneAndStripHalProperties(HalResource hal) {
    ObjectNode clone = hal.getModel().deepCopy();
    clone.remove("_links");
    clone.remove("_embedded");
    return clone;
  }
}
