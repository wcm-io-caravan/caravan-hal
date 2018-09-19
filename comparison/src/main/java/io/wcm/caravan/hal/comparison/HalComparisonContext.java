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
package io.wcm.caravan.hal.comparison;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.hal.resource.HalResource;

/**
 * The "relational" location of a resource (or property) within a tree of {@link HalResource}s, constructed
 * by concatenating the relations and JSON properties to follow from the API entry point.
 */
@ProviderType
public interface HalComparisonContext {

  /**
   * @return the name of the last HAL relation in the path
   */
  String getLastRelation();

  /**
   * @return all relations that were followed to get to the context resource
   */
  List<String> getAllRelations();

  /**
   * @param relation that was used to embed/link to the resource which is to be found
   * @return the nearest parent of the current context that was linked or embedded with that relation
   */
  HalResource getParentResourceWithRelation(String relation);


  /**
   * @return the name of the JSON property where a difference was detected (or empty string, if the difference was not
   *         in a JSON property)
   */
  String getLastProperyName();

  /**
   * @return the names of all JSON properties contained in the HAL path (as returned by {@link #toString()}
   */
  List<String> getAllPropertyNames();

  /**
   * @return the URL where the resource that contains the expected value can be loaded from
   */
  String getExpectedUrl();

  /**
   * @return the URL where the resource that contains the actual value can be loaded from
   */
  String getActualUrl();

  /**
   * @return a string representation of the full HAL path (e.g. <code>/section[1]/item[0]</code> for the first item in
   *         the second section linked or embbedded from the entry point)
   */
  @Override
  String toString();

}
