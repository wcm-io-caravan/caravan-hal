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

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.difference.HalDifferenceListBuilder;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingStep;
import io.wcm.caravan.hal.comparison.impl.matching.MatchingAlgorithm;
import io.wcm.caravan.hal.comparison.impl.matching.MatchingResult;
import io.wcm.caravan.hal.comparison.impl.matching.SimpleIdentityMatchingAlgorithm;
import io.wcm.caravan.hal.resource.HalResource;


public class EmbeddedReorderingDetector implements EmbeddedProcessingStep {

  private final DefaultIdProvider defaultIdProvider = new DefaultIdProvider();

  private final HalComparisonStrategy strategy;

  /**
   * @param strategy that defines equivalence for specific hal resources
   */
  public EmbeddedReorderingDetector(HalComparisonStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public List<HalDifference> apply(HalComparisonContext context, List<HalResource> expected, List<HalResource> actual) {

    MatchingResult<HalResource> matchingResult = applyMatchingAlgorithm(context, expected, actual);

    // if not a single match that was found, then either one of the lists is empty, or all items
    // are considered to be different. In that case it's better to compare them in the original order,
    // then just to treat them all as added and removed, so we return early without reporting any diff
    if (matchingResult.getMatchedActual().isEmpty()) {
      return Collections.emptyList();
    }

    // collect the re-ordering, addition and removal differences
    HalDifferenceListBuilder diffs = findDifferences(context, expected, actual, matchingResult);

    // all following processing steps should only process the embedded resources that were successfully
    // matched (and re-ordered), so the given lists will be completely replaced with the ones from the
    // matching result
    replaceItems(expected, matchingResult.getMatchedExpected());
    replaceItems(actual, matchingResult.getMatchedActual());

    return diffs.build();
  }

  private MatchingResult<HalResource> applyMatchingAlgorithm(HalComparisonContext context, List<HalResource> expected, List<HalResource> actual) {

    Function<HalResource, String> idProvider = defaultIfNull(strategy.getIdProvider(context), defaultIdProvider);

    MatchingAlgorithm<HalResource> algorithm = new SimpleIdentityMatchingAlgorithm<HalResource>(idProvider);

    return algorithm.findMatchingItems(expected, actual);
  }

  private HalDifferenceListBuilder findDifferences(HalComparisonContext context, List<HalResource> expected, List<HalResource> actual,
      MatchingResult<HalResource> matchingResult) {

    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);
    String relation = context.getLastRelation();
    boolean reorderingRequired = matchingResult.areMatchesReordered();

    if (reorderingRequired) {
      String msg = "The embedded " + relation + " resources have a different order in the actual resource";
      diffs.reportReorderedEmbedded(msg, expected, actual);
    }

    for (HalResource removed : matchingResult.getRemovedExpected()) {
      String msg = "An embedded " + relation + getResourceTitle(removed) + "is missing in the actual resource";
      diffs.reportMissingEmbedded(msg, removed);
    }

    for (HalResource added : matchingResult.getAddedActual()) {
      String msg = "An additional embedded " + relation + getResourceTitle(added) + "is present in the actual resource";
      diffs.reportAdditionalEmbedded(msg, added);
    }

    return diffs;
  }

  private static String getResourceTitle(HalResource hal) {
    ObjectNode removedJson = hal.getModel();
    String title = StringUtils.defaultIfEmpty(removedJson.path("title").asText(), removedJson.path("name").asText());
    String label = StringUtils.isNotBlank(title) ? " '" + title + "' " : " ";
    return label;
  }

  private static void replaceItems(List<HalResource> original, List<HalResource> remaining) {
    original.clear();

    original.addAll(remaining);
  }

  /**
   * If {@link HalComparisonStrategy#getIdProvider(HalComparisonContext)} is not implemented by the consumer,
   * the default behaviour is to identify embedded resources by their title property
   */
  static class DefaultIdProvider implements Function<HalResource, String> {

    @Override
    public String apply(HalResource resource) {
      return resource.getModel().path("title").asText();
    }
  }
}
