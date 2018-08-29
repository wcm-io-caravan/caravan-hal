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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A user-defined configuration for the {@link HalComparison} service that can be implemented dynamically and
 * context-aware.
 * <p>
 * With the default implementations in this interface, the comparison will run as long as the crawler finds
 * more links that haven't been processed yet, and will compare all embedded relations.
 * Depending on your data structures, it is essential to restrict crawling to specific relations by implementing
 * the {@link #ignoreLinkTo(HalPath)} method.
 * </p>
 */
@ConsumerType
public interface HalComparisonConfig {

  /**
   * @param halPath the relational location of the embedded resource to be compared
   * @return true if that resource (and everything below) should not be compared
   */
  default boolean ignoreEmbeddedAt(HalPath halPath) {
    return false;
  }

  /**
   * @param halPath the relational location of a linked resource to be followed
   * @return true if that resource (and everything below) should not be followed &amp; compared
   */
  default boolean ignoreLinkTo(HalPath halPath) {
    return false;
  }

  // TODO add more configuration options
  // - limit the maximum depth of crawling
  // - allow to specify which link templates should be expanded (with which values)
  // - allow to specify for which links/resources can be found in random order
}
