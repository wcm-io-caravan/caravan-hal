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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalDifferenceImpl;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingStep;
import io.wcm.caravan.hal.comparison.impl.embedded.steps.IdentityMatchingAlgorithm.ItemWithIndex;
import io.wcm.caravan.hal.comparison.impl.embedded.steps.IdentityMatchingAlgorithm.MatchingResult;
import io.wcm.caravan.hal.comparison.impl.util.HalStringConversion;
import io.wcm.caravan.hal.resource.HalResource;


public class EmbeddedReorderingDetector implements EmbeddedProcessingStep {

  private final IdentityMatchingAlgorithm matching = new IdentityMatchingAlgorithm();
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

    String relation = context.getLastRelation();
    List<HalDifference> diffs = new ArrayList<>();

    Function<HalResource, String> idProvider = ObjectUtils.defaultIfNull(strategy.getIdProvider(context), defaultIdProvider);
    MatchingResult matchingResult = matching.match(expected, actual, idProvider);

    if (matchingResult.getMatchedActual().isEmpty()) {
      return Collections.emptyList();
    }

    boolean reorderingRequired = matchingResult.areMatchesReordered();

    if (reorderingRequired) {
      diffs.add(new HalDifferenceImpl(context, null, null,
          "The embedded " + relation + " resources have a different order in the actual resource"));
    }

    for (ItemWithIndex removed : matchingResult.getRemovedExpected()) {
      HalResource removedHal = removed.getItem();

      diffs.add(new HalDifferenceImpl(context, asString(removedHal), null,
          "An embedded " + relation + getResourceTitle(removedHal) + "is missing in the actual resource"));
    }

    for (ItemWithIndex added : matchingResult.getAddedActual()) {
      HalResource addedHal = added.getItem();
      diffs.add(new HalDifferenceImpl(context, null, asString(addedHal),
          "An additional embedded " + relation + getResourceTitle(addedHal) + "is present in the actual resource"));
    }

    replaceItems(expected, matchingResult.getMatchedExpected());
    replaceItems(actual, matchingResult.getMatchedActual());

    return diffs;
  }

  private static String getResourceTitle(HalResource hal) {
    ObjectNode removedJson = hal.getModel();
    String title = StringUtils.defaultIfEmpty(removedJson.path("title").asText(), removedJson.path("name").asText());
    String label = StringUtils.isNotBlank(title) ? " '" + title + "' " : " ";
    return label;
  }

  static void replaceItems(List<HalResource> original, List<ItemWithIndex> remaining) {
    original.clear();

    remaining.stream()
        .map(ItemWithIndex::getItem)
        .forEach(item -> original.add(item));
  }

  static class DefaultIdProvider implements Function<HalResource, String> {

    @Override
    public String apply(HalResource resource) {
      ObjectNode stateJson = HalStringConversion.cloneAndStripHalProperties(resource.getModel());
      return stateJson.toString();
    }
  }
}
