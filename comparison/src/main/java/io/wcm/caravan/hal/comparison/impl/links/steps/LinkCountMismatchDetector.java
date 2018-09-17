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
package io.wcm.caravan.hal.comparison.impl.links.steps;

import java.util.Collections;
import java.util.List;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.difference.HalDifferenceListBuilder;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessingStep;
import io.wcm.caravan.hal.resource.Link;


/**
 * Detects if the number of links for a specific relation is different
 */
public class LinkCountMismatchDetector implements LinkProcessingStep {

  @Override
  public List<HalDifference> apply(HalComparisonContext context, List<Link> expected, List<Link> actual) {

    if (expected.size() == actual.size()) {
      return Collections.emptyList();
    }

    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);

    String msg = "Expected " + expected.size() + " '" + context.getLastRelation() + "' links, but found " + actual.size();

    if (expected.size() > actual.size()) {
      diffs.reportMissingLinks(msg, expected, actual);
    }
    else {
      diffs.reportAdditionalLinks(msg, expected, actual);
    }

    return diffs.build();
  }
}
