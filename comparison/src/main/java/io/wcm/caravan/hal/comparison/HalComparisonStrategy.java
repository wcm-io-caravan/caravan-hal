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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * A user-defined configuration strategy for the {@link HalComparison} service that can be implemented dynamically and
 * context-aware.
 * <p>
 * With the default implementations in this interface, the comparison will run as long as the crawler finds
 * more links that haven't been processed yet, and will compare all embedded relations.
 * Depending on your data structures, it is essential to restrict crawling to specific relations by implementing
 * the {@link #ignoreLinkTo(HalComparisonContext)} method.
 * </p>
 */
@ConsumerType
public interface HalComparisonStrategy {

  /**
   * Can be implemented by the service consumer to exclude specific embedded resources from comparison.
   * @param halContext the relational location of the embedded resource to be compared
   * @return true if that resource (and everything below) should not be compared
   */
  default boolean ignoreEmbeddedAt(HalComparisonContext halContext) {
    return false;
  }

  /**
   * Can be implemented by the service consumer to exclude specific links from crawling and comparison.
   * @param halContext the relational location of a linked resource to be followed
   * @return true if that resource (and everything below) should not be followed &amp; compared
   */
  default boolean ignoreLinkTo(HalComparisonContext halContext) {
    return false;
  }

  /**
   * Can be implemented by the service consumer to expand specific link templates, so that the expanded link is then
   * crawled and included in the comparison. By default, link templates are not expanded and won't be
   * crawled (only the template variables will be compared).
   * @param context the relational location of a link template
   * @param expectedLink the full link template as found in the expected resource tree
   * @param actualLink the full link template as found in the actual resource tree
   * @return a list that contains one map of variables for each expanded instance of this link template that should be
   *         created
   */
  default List<Map<String, Object>> getVariablesToExpandLinkTemplate(HalComparisonContext context, Link expectedLink, Link actualLink) {
    return Collections.emptyList();
  }

  /**
   * Can be implemented by the service consumer to improve the detection of additional, removed and re-ordered embedded
   * resources. By default, the "title" property of the embedded resources is used to find matching pairs. If your
   * resources have some property that is unique in the context (and unlikely to change between environments),
   * you should return a function that extracts that property's value.
   * @param context the relational location of an embedded resource
   * @return a function to extract an id that can be used to determine whether a specific resource was
   *         added/removed/reordered. If null is returned, the matching algorithm will use the title property
   */
  default Function<HalResource, String> getIdProvider(HalComparisonContext context) {
    return null;
  }

}
