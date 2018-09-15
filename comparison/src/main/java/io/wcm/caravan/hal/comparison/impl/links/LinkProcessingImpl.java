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
package io.wcm.caravan.hal.comparison.impl.links;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.PairWithRelation;
import io.wcm.caravan.hal.comparison.impl.ProcessingResult;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.links.steps.AdditionalOrMissingNamedLinkDetector;
import io.wcm.caravan.hal.comparison.impl.links.steps.LinkCountMismatchDetector;
import io.wcm.caravan.hal.comparison.impl.links.steps.LinkRelationBlackList;
import io.wcm.caravan.hal.comparison.impl.links.steps.LinkTemplateProcessor;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Implementation of {@link LinkProcessing} that delegates the actual logic to multiple
 * {@link LinkProcessingStep}s (and collects the results).
 */
public class LinkProcessingImpl implements LinkProcessing {

  private final Iterable<LinkProcessingStep> processingSteps;

  /**
   * Default constructor that defines the order of processing steps to be executed
   */
  public LinkProcessingImpl(HalComparisonStrategy strategy) {
    this.processingSteps = ImmutableList.of(
        new LinkRelationBlackList(strategy),
        new LinkCountMismatchDetector(),
        new AdditionalOrMissingNamedLinkDetector(),
        new LinkTemplateProcessor(strategy));
  }

  /**
   * Alternative constructor that allows mocking of processing steps
   * @param processingSteps to be applied *instead* of the default processing steps
   */
  public LinkProcessingImpl(Iterable<LinkProcessingStep> processingSteps) {
    this.processingSteps = ImmutableList.copyOf(processingSteps);
  }

  @Override
  public ProcessingResult<Link> process(HalComparisonContextImpl context, HalResource expected, HalResource actual) {

    List<PairWithRelation<Link>> pairsToCompare = new ArrayList<>();
    List<HalDifference> diffs = new ArrayList<>();

    ListMultimap<String, Link> allExpectedLinks = expected.getLinks();
    ListMultimap<String, Link> allActualLinks = actual.getLinks();

    Set<String> allRelations = Sets.union(allExpectedLinks.keys().elementSet(), allActualLinks.keys().elementSet());

    for (String relation : allRelations) {

      HalComparisonContext newContext = context.withAppendedHalPath(relation);

      List<Link> remainingExpectedLinks = new ArrayList<>(allExpectedLinks.get(relation));
      List<Link> remainingActualLinks = new ArrayList<>(allActualLinks.get(relation));

      for (LinkProcessingStep step : processingSteps) {
        diffs.addAll(step.apply(newContext, remainingExpectedLinks, remainingActualLinks));
      }

      for (int i = 0; i < remainingExpectedLinks.size() && i < remainingActualLinks.size(); i++) {
        pairsToCompare.add(new PairWithRelation<Link>(relation, remainingExpectedLinks.get(i), remainingActualLinks.get(i)));
      }
    }

    return new ProcessingResult<>(pairsToCompare, diffs);
  }

}
