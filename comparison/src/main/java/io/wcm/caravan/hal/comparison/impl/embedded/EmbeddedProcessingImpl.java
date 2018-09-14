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
package io.wcm.caravan.hal.comparison.impl.embedded;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.PairWithRelation;
import io.wcm.caravan.hal.comparison.impl.ProcessingResult;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.embedded.steps.EmbeddedCountMismatchDetector;
import io.wcm.caravan.hal.comparison.impl.embedded.steps.EmbeddedRelationBlackList;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Implementation of {@link EmbeddedProcessing} that delegates the actual logic to multiple
 * {@link EmbeddedProcessingStep}s (and collects the results).
 */
public class EmbeddedProcessingImpl implements EmbeddedProcessing {

  private final Iterable<EmbeddedProcessingStep> processingSteps;

  /**
   * Default constructor that defines the order of processing steps to be executed
   */
  public EmbeddedProcessingImpl() {
    this.processingSteps = ImmutableList.of(
        new EmbeddedRelationBlackList(),
        new EmbeddedCountMismatchDetector());
  }

  /**
   * Alternative constructor that allows mocking of processing steps
   * @param processingSteps to be applied *instead* of the default processing steps
   */
  public EmbeddedProcessingImpl(Iterable<EmbeddedProcessingStep> processingSteps) {
    this.processingSteps = ImmutableList.copyOf(processingSteps);
  }

  @Override
  public ProcessingResult<HalResource> process(HalComparisonContextImpl context, HalResource expected, HalResource actual) {

    List<PairWithRelation<HalResource>> pairsToCompare = new ArrayList<>();
    List<HalDifference> diffs = new ArrayList<>();

    ListMultimap<String, HalResource> allExpectedResources = expected.getEmbedded();
    ListMultimap<String, HalResource> allActualResources = actual.getEmbedded();

    Set<String> allRelations = Sets.union(allExpectedResources.keys().elementSet(), allActualResources.keys().elementSet());

    for (String relation : allRelations) {

      HalComparisonContextImpl newContext = context.withAppendedHalPath(relation);

      List<HalResource> remainingExpectedResources = new ArrayList<>(allExpectedResources.get(relation));
      List<HalResource> remainingActualResources = new ArrayList<>(allActualResources.get(relation));

      for (EmbeddedProcessingStep step : processingSteps) {
        diffs.addAll(step.apply(newContext, relation, remainingExpectedResources, remainingActualResources));
      }

      for (int i = 0; i < remainingExpectedResources.size() && i < remainingActualResources.size(); i++) {
        pairsToCompare.add(new PairWithRelation<>(relation, remainingExpectedResources.get(i), remainingActualResources.get(i)));
      }
    }

    return new ProcessingResult<>(pairsToCompare, diffs);
  }

}
