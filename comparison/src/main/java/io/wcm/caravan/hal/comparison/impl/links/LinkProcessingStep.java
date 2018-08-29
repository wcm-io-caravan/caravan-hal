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
package io.wcm.caravan.hal.comparison.impl.links;

import java.util.List;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalComparisonContext;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingImpl;
import io.wcm.caravan.hal.resource.Link;

/**
 * A processing step implements one aspect of the pre-processing of links in
 * {@link EmbeddedProcessingImpl}, by performing one or more of the following actions:
 * <ul>
 * <li>find any differences in the two given lists *without actually loading the linked resources*</li>
 * <li>remove items from the list (to ignore specific resources in the comparison)</li>
 * <li>re-order the list (to avoid unreadable diffs in lists where the order isn't guaranteed or items have been
 * added/removed</li>
 * </ul>
 */
public interface LinkProcessingStep {

  /**
   * @param context specifies the 'relational' location of the given resources (and provides access to configuration)
   * @param relation of the given links in their context resource
   * @param expected a *mutable* list that the step can reorder or reduce (if specific resources shouldn't be compared)
   * @param actual a *mutable* list that the step can reorder or reduce (if specific resources shouldn't be compared)
   * @return a list of all {@link HalDifference} that were detected
   */
  List<HalDifference> apply(HalComparisonContext context, String relation, List<Link> expected, List<Link> actual);
}
