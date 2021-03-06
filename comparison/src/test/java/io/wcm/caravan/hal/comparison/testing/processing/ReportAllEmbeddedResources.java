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

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.difference.HalDifferenceListBuilder;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingStep;
import io.wcm.caravan.hal.resource.HalResource;

public final class ReportAllEmbeddedResources implements EmbeddedProcessingStep {

  private List<HalDifference> diffsForAllRelations = new ArrayList<>();

  @Override
  public List<HalDifference> apply(HalComparisonContextImpl context, List<HalResource> e, List<HalResource> a) {
    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);

    for (HalResource hal : e) {
      diffs.reportModifiedEmbedded("flagged by " + getClass().getSimpleName(), hal, null);
    }

    diffsForAllRelations.addAll(diffs.build());

    return diffs.build();
  }

  public List<HalDifference> getReportedDiffs() {
    return diffsForAllRelations;
  }
}
