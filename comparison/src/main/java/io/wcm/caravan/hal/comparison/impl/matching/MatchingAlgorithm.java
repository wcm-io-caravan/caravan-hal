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
 * An algorithm that finds matching pairs in the two given lists
 * @param <T> the type of the items to match
 */
public interface MatchingAlgorithm<T> {

  /**
   * @param expectedList
   * @param actualList
   * @return a {@link MatchingResult} instance
   */
  MatchingResult<T> findMatchingItems(List<T> expectedList, List<T> actualList);

}