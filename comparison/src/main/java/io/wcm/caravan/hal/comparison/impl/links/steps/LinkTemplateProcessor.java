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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.collect.Sets;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.difference.HalDifferenceListBuilder;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessingStep;
import io.wcm.caravan.hal.resource.Link;

/**
 * Detects if the available in a pair of link templates is different, and filteres all link-templates so they are not
 * being followed in the recursive comparison.
 */
public class LinkTemplateProcessor implements LinkProcessingStep {

  private final HalComparisonStrategy strategy;

  /**
   * @param strategy that defines which relations should be ignored
   */
  public LinkTemplateProcessor(HalComparisonStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public List<HalDifference> apply(HalComparisonContextImpl context, List<Link> expected, List<Link> actual) {

    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);

    List<Link> expectedExpandedTemplates = new ArrayList<>();
    List<Link> actualExpandedTemplates = new ArrayList<>();

    Iterator<Link> expectedIt = expected.iterator();
    Iterator<Link> actualIt = actual.iterator();
    while (expectedIt.hasNext() && actualIt.hasNext()) {
      Link expectedLink = expectedIt.next();
      Link actualLink = actualIt.next();

      // whenever one of the links is templated...
      if (expectedLink.isTemplated() || actualLink.isTemplated()) {

        // return a result when there is a difference in the template parameters
        findTemplateDifferences(context, expectedLink, actualLink, diffs);

        List<Map<String, Object>> variables = strategy.getVariablesToExpandLinkTemplate(context, expectedLink, actualLink);
        if (variables != null) {
          for (Map<String, Object> map : variables) {
            expectedExpandedTemplates.add(createExpandedLink(expectedLink, map));
            actualExpandedTemplates.add(createExpandedLink(actualLink, map));
          }
        }

        // and finally remove the links from the list of links to compare
        expectedIt.remove();
        actualIt.remove();
      }
    }

    // there could be more expected than actual links (or vice versa)
    filterTemplatesFromRemaining(expectedIt);
    filterTemplatesFromRemaining(actualIt);

    expected.addAll(expectedExpandedTemplates);
    actual.addAll(actualExpandedTemplates);

    return diffs.build();
  }

  private static Link createExpandedLink(Link linkTemplate, Map<String, Object> variables) {
    String name = linkTemplate.getName();
    UriTemplate template = UriTemplate.fromTemplate(linkTemplate.getHref());
    String expandedUrl = template.expand(variables);
    Link expandedLink = new Link(expandedUrl);
    if (name == null) {
      expandedLink.setName(variables.toString());
    }
    return expandedLink;
  }

  private static void filterTemplatesFromRemaining(Iterator<Link> remainingIt) {
    while (remainingIt.hasNext()) {
      Link link = remainingIt.next();
      if (link.isTemplated()) {
        remainingIt.remove();
      }
    }
  }

  private static String formatNames(Collection<String> items) {
    return items.stream()
        .collect(Collectors.joining(", ", "[", "]"));
  }

  private static String formatTemplateVariables(Link link) {
    Set<String> variables = Sets.newHashSet(UriTemplate.fromTemplate(link.getHref()).getVariables());
    return formatNames(variables);
  }

  private static void findTemplateDifferences(HalComparisonContext context, Link expected, Link actual, HalDifferenceListBuilder diffs) {

    if (expected.isTemplated() != actual.isTemplated()) {
      String msg = expected.isTemplated()
          ? "Expected templated link with variables " + formatTemplateVariables(expected) + ", but found resolved link"
          : "Expected resolved link, but found template with variables " + formatTemplateVariables(actual);
      diffs.reportModifiedLink(msg, expected, actual);
      return;
    }

    UriTemplate expectedTemplate = UriTemplate.fromTemplate(expected.getHref());
    UriTemplate actualTemplate = UriTemplate.fromTemplate(actual.getHref());

    Set<String> expectedVariables = Sets.newHashSet(expectedTemplate.getVariables());
    Set<String> actualVariables = Sets.newHashSet(actualTemplate.getVariables());

    Set<String> missingVariables = Sets.difference(expectedVariables, actualVariables);
    Set<String> additionalVariables = Sets.difference(actualVariables, expectedVariables);

    if (!missingVariables.isEmpty() || !additionalVariables.isEmpty()) {
      String msg = "Expected template parameters to be " + formatNames(expectedVariables) + ","
          + " but found " + formatNames(actualVariables);
      diffs.reportModifiedLink(msg, expected, actual);
    }
  }

}
