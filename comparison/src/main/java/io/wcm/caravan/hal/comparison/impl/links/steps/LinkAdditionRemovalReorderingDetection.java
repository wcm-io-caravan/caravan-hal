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

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.difference.HalDifferenceListBuilder;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessingStep;
import io.wcm.caravan.hal.comparison.impl.matching.MatchingAlgorithm;
import io.wcm.caravan.hal.comparison.impl.matching.MatchingResult;
import io.wcm.caravan.hal.comparison.impl.matching.SimpleIdentityMatchingAlgorithm;
import io.wcm.caravan.hal.resource.Link;


public class LinkAdditionRemovalReorderingDetection implements LinkProcessingStep {

  @Override
  public List<HalDifference> apply(HalComparisonContext context, List<Link> expected, List<Link> actual) {

    MatchingResult<Link> matchingResult = applyMatchingAlgorithm(context, expected, actual);

    // collect the re-ordering, addition and removal differences
    HalDifferenceListBuilder diffs = findDifferences(context, expected, actual, matchingResult);

    // all following processing steps should only process the lniks that were successfully
    // matched (and re-ordered), so the given lists will be completely replaced with the ones from the
    // matching result
    expected.clear();
    expected.addAll(matchingResult.getMatchedExpected());

    actual.clear();
    actual.addAll(matchingResult.getMatchedActual());

    return diffs.build();
  }

  private MatchingResult<Link> applyMatchingAlgorithm(HalComparisonContext context, List<Link> expected, List<Link> actual) {

    Function<Link, String> idProvider = link -> link.getName();

    MatchingAlgorithm<Link> algorithm = new SimpleIdentityMatchingAlgorithm<>(idProvider);

    return algorithm.findMatchingItems(expected, actual);
  }

  private HalDifferenceListBuilder findDifferences(HalComparisonContext context, List<Link> expected, List<Link> actual,
      MatchingResult<Link> matchingResult) {

    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);
    String relation = context.getLastRelation();
    boolean reorderingRequired = matchingResult.areMatchesReordered();

    if (reorderingRequired) {
      String msg = "The embedded " + relation + " resources have a different order in the actual resource";
      diffs.reportReorderedLinks(msg, expected, actual);
    }

    for (Link removed : matchingResult.getRemovedExpected()) {
      String msg = "An embedded " + relation + getLinkNameOrTitle(removed) + "is missing in the actual resource";
      diffs.reportMissingLink(msg, removed);
    }

    for (Link added : matchingResult.getAddedActual()) {
      String msg = "An additional embedded " + relation + getLinkNameOrTitle(added) + "is present in the actual resource";
      diffs.reportAdditionalLink(msg, added);
    }

    return diffs;
  }

  private static String getLinkNameOrTitle(Link link) {

    String nameOrTitle = StringUtils.defaultIfEmpty(link.getName(), link.getTitle());
    String label = StringUtils.isNotBlank(nameOrTitle) ? " '" + nameOrTitle + "' " : " ";
    return label;
  }
}
