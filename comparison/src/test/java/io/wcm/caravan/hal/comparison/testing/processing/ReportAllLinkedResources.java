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
package io.wcm.caravan.hal.comparison.testing.processing;

import static io.wcm.caravan.hal.comparison.testing.StandardRelations.SELF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalDifferenceImpl;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessingStep;
import io.wcm.caravan.hal.resource.Link;

public final class ReportAllLinkedResources implements LinkProcessingStep {

  private List<HalDifference> diffsForAllRelations = new ArrayList<>();

  @Override
  public List<HalDifference> apply(HalComparisonContext context, String relation, List<Link> e, List<Link> a) {

    if (SELF.equals(relation)) {
      return Collections.emptyList();
    }

    List<HalDifference> diffsForThisRelation = e.stream()
        .map(r -> new HalDifferenceImpl(context, null, null, null))
        .collect(Collectors.toList());

    diffsForAllRelations.addAll(diffsForThisRelation);
    return diffsForThisRelation;
  }

  public List<HalDifference> getReportedDiffs() {
    return diffsForAllRelations;
  }
}
