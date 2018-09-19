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
package io.wcm.caravan.hal.comparison.impl.embedded.steps;

import java.util.Collections;
import java.util.List;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingStep;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Ignore specific embedded resources as configured in
 * {@link HalComparisonStrategy#ignoreEmbeddedAt(HalComparisonContext)}
 */
public class EmbeddedRelationBlackList implements EmbeddedProcessingStep {

  private final HalComparisonStrategy strategy;

  /**
   * @param strategy that defines which relations should be ignored
   */
  public EmbeddedRelationBlackList(HalComparisonStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public List<HalDifference> apply(HalComparisonContextImpl context, List<HalResource> expected, List<HalResource> actual) {

    boolean blackListed = strategy.ignoreEmbeddedAt(context);
    if (blackListed) {
      expected.clear();
      actual.clear();
    }

    return Collections.emptyList();
  }

}
