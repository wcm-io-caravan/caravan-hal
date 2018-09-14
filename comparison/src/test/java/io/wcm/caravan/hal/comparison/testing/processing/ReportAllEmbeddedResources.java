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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.HalDifferenceImpl;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingStep;
import io.wcm.caravan.hal.resource.HalResource;

public final class ReportAllEmbeddedResources implements EmbeddedProcessingStep {

  private List<HalDifference> diffsForAllRelations = new ArrayList<>();

  @Override
  public List<HalDifference> apply(HalComparisonContextImpl context, String relation, List<HalResource> e, List<HalResource> a) {

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