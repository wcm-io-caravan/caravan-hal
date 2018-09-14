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

import static io.wcm.caravan.hal.comparison.impl.util.HalStringConversion.asString;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalDifferenceImpl;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingStep;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Detects if the number of embedded resources for a specific relation is different
 */
public class EmbeddedCountMismatchDetector implements EmbeddedProcessingStep {

  @Override
  public List<HalDifference> apply(HalComparisonContext context, String relation, List<HalResource> expected,
      List<HalResource> actual) {

    if (expected.size() == actual.size()) {
      return Collections.emptyList();
    }

    String msg = "Expected " + expected.size() + " embedded '" + relation + "' resources, but found " + actual.size();
    return ImmutableList.of(new HalDifferenceImpl(context, asString(expected), asString(actual), msg));
  }

}
