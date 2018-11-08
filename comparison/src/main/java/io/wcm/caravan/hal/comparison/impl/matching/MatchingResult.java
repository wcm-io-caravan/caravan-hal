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
package io.wcm.caravan.hal.comparison.impl.matching;

import java.util.List;

/**
 * Result class for {@link MatchingAlgorithm}s
 * @param <T> the type of the items to be matched
 */
public interface MatchingResult<T> {

  /**
   * @return a list of all expected items (in the original order) for which a matching actual item was found
   */
  List<T> getMatchedExpected();

  /**
   * @return a list of all matched actual items, ordered so that each item is at the same position as the corresponding
   *         expected item
   */
  List<T> getMatchedActual();

  /**
   * @return a list of all expected items for which no matching actual item was found
   */
  List<T> getRemovedExpected();

  /**
   * @return a list of all actual items for which no matching expected item was found
   */
  List<T> getAddedActual();

  /**
   * @return true if one or more of the matched actual items were in a difference order than the corresponding
   *         expected items
   */
  boolean areMatchesReordered();

}
