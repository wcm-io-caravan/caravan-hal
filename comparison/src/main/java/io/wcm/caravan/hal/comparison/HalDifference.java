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

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.hal.resource.HalResource;

/**
 * Represents a single difference detected by the {@link HalComparison} when comparing two trees of
 * {@link HalResource}s.
 * <p>
 * Some examples of changes that are represented by a {@link HalDifference} instance:
 * </p>
 * <ul>
 * <li>a JSON property in the resource state was added, removed or modified</li>
 * <li>a link was added or removed</li>
 * <li>an embedded resource was added or removed</li>
 * <li>the order of links or embedded resource has changed</li>
 * </ul>
 */
@ProviderType
public interface HalDifference {

  /**
   * Describes the "relational" location of the resource (or property) that was reported to be different.
   * @return a {@link HalComparisonContext} that can be used to group or filter the results based on their relations and
   *         context
   */
  HalComparisonContext getHalContext();

  /**
   * @return a text that describes why the difference was reported
   */
  String getDescription();

  // TODO: add getters for the "change type" (add/remove/update/reorder) and "object type" (property, resource, link)
  // that allow easy filtering of the results depending on the use case

  /**
   * @return a formatted JSON string of the resource, link, property or state object that was expected (or an empty
   *         string if the value only exists in the actual resource)
   */
  String getExpectedJson();

  /**
   * @return a formatted JSON string of the resource, link, property or state object that was actually found (or an
   *         empty string if the value only exists in the expected resource)
   */
  String getActualJson();
}
