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

import static io.wcm.caravan.hal.comparison.impl.util.HalStringConversion.asString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalComparisonContext;
import io.wcm.caravan.hal.comparison.impl.HalDifferenceImpl;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessingStep;
import io.wcm.caravan.hal.resource.Link;

/**
 * Uses the (optional) name of links to determine which of the links have been added or removed - this
 * avoids unreadable diffs that can happen if they are compared based on there indices.
 */
public class AdditionalOrMissingNamedLinkDetector implements LinkProcessingStep {

  @Override
  public List<HalDifference> apply(HalComparisonContext context, String relation, List<Link> expected, List<Link> actual) {

    boolean thereAreLinksWithoutNames = Stream.concat(expected.stream(), actual.stream())
        .anyMatch(link -> link.getName() == null);

    boolean namesAreUnique = expected.stream().map(Link::getName).distinct().count() == expected.size()
        && actual.stream().map(Link::getName).distinct().count() == actual.size();

    if (thereAreLinksWithoutNames || !namesAreUnique) {
      return Collections.emptyList();
    }

    List<Link> unexpectedLinks = findItemsNotPresentIn(expected, actual);
    List<Link> missingLinks = findItemsNotPresentIn(actual, expected);

    List<HalDifference> diffs = new ArrayList<>();

    for (Link link : missingLinks) {
      expected.remove(link);
      String msg = "Expected '" + relation + "' link with name '" + link.getName() + "' is missing";
      diffs.add(new HalDifferenceImpl(context, asString(link), null, msg));
    }

    for (Link link : unexpectedLinks) {
      actual.remove(link);
      String msg = "Found an unexpected '" + relation + "' link with name '" + link.getName() + "'";
      diffs.add(new HalDifferenceImpl(context, null, asString(link), msg));
    }

    return diffs;
  }

  List<Link> findItemsNotPresentIn(List<Link> toLookIn, List<Link> toLookFor) {

    Set<String> namesToIgnore = toLookIn.stream()
        .map(Link::getName)
        .collect(Collectors.toSet());

    List<Link> addedItems = toLookFor.stream()
        .filter(link -> !namesToIgnore.contains(link.getName()))
        .collect(Collectors.toList());

    return addedItems;
  }
}
