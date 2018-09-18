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

import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.MODIFIED;
import static io.wcm.caravan.hal.comparison.HalDifference.EntityType.LINK;
import static io.wcm.caravan.hal.comparison.testing.HalDifferenceAssertions.assertOnlyOneDifference;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.ITEM;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.testing.TestHalComparisonContext;
import io.wcm.caravan.hal.comparison.testing.TestHalComparisonStrategy;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class LinkTemplateExpansionTest {

  private HalComparisonStrategy strategy;

  private List<Link> createLinks(String... paths) {
    return Stream.of(paths)
        .map(Link::new)
        .collect(Collectors.toList());
  }

  @SafeVarargs
  private final void mockExpansionStrategyWithVariables(Map<String, Object>... variables) {
    strategy = new HalComparisonStrategy() {

      @Override
      public List<Map<String, Object>> getVariablesToExpandLinkTemplate(HalComparisonContext context, Link expectedLink, Link actualLink) {
        return Lists.newArrayList(variables);
      }

    };
  }

  private List<HalDifference> findDifferences(List<Link> expected, List<Link> actual) {

    // use the default strategy unless a specific strategy was defined in the test
    if (strategy == null) {
      strategy = new TestHalComparisonStrategy();
    }

    LinkTemplateProcessor processor = new LinkTemplateProcessor(strategy);

    // the LinkProcessingImpl will already add the relation to the context before calling its steps, so we do the same here
    HalComparisonContext context = new TestHalComparisonContext().withAppendedHalPath(ITEM, new HalResource());

    return processor.apply(context, expected, actual);
  }

  @Test
  public void strategy_should_not_be_called_for_pair_of_resolved_links() throws Exception {

    List<Link> expected = createLinks("/resolved");
    List<Link> actual = createLinks("/resolved");

    this.strategy = new HalComparisonStrategy() {

      @Override
      public List<Map<String, Object>> getVariablesToExpandLinkTemplate(HalComparisonContext context, Link expectedLink, Link actualLink) {
        throw new AssertionError();
      }
    };

    findDifferences(expected, actual);
  }

  @Test
  public void templates_should_be_filtered_instead_of_expanded_if_default_strategy_is_used() throws Exception {

    List<Link> expected = createLinks("/expected{?a,b}");
    List<Link> actual = createLinks("/actual/{a}/{b}");

    findDifferences(expected, actual);

    assertThat(expected, hasSize(0));
    assertThat(actual, hasSize(0));
  }

  @Test
  public void templates_should_be_filtered_instead_of_expanded_if_no_variables_are_specified() throws Exception {

    List<Link> expected = createLinks("/expected{?a,b}");
    List<Link> actual = createLinks("/actual/{a}/{b}");

    mockExpansionStrategyWithVariables();

    findDifferences(expected, actual);

    assertThat(expected, hasSize(0));
    assertThat(actual, hasSize(0));
  }

  @Test
  public void templates_should_be_expanded_if_present_in_expected_and_actual_links() throws Exception {

    List<Link> expected = createLinks("/expected{?a,b}");
    List<Link> actual = createLinks("/actual/{a}/{b}");

    mockExpansionStrategyWithVariables(
        ImmutableMap.of("a", "A1", "b", "B1"),
        ImmutableMap.of("a", "A2", "b", "B2"));

    findDifferences(expected, actual);

    assertThat(expected, hasSize(2));
    assertThat(expected.get(0).getHref(), equalTo("/expected?a=A1&b=B1"));
    assertThat(expected.get(1).getHref(), equalTo("/expected?a=A2&b=B2"));

    assertThat(actual, hasSize(2));
    assertThat(actual.get(0).getHref(), equalTo("/actual/A1/B1"));
    assertThat(actual.get(1).getHref(), equalTo("/actual/A2/B2"));
  }


  @Test
  public void templates_should_be_expanded_if_only_present_in_expected() throws Exception {

    List<Link> expected = createLinks("/expected{?flag}");
    List<Link> actual = createLinks("/actual?flag=true");

    mockExpansionStrategyWithVariables(ImmutableMap.of("flag", true));

    List<HalDifference> diffs = findDifferences(expected, actual);

    // the fact that only one link is templated is still considered a difference!
    assertOnlyOneDifference(diffs, MODIFIED, LINK, "/item");

    // but what's important is that with defining those values, it's now possible to continue crawling
    // and comparing, because the link template was expanded to be identical to the resolved link
    assertThat(expected, hasSize(1));
    assertThat(expected.get(0).getHref(), equalTo("/expected?flag=true"));

    assertThat(actual, hasSize(1));
    assertThat(actual.get(0).getHref(), equalTo("/actual?flag=true"));
  }

  @Test
  public void templates_should_be_expanded_if_only_present_in_actual() throws Exception {

    List<Link> expected = createLinks("/expected?flag=true");
    List<Link> actual = createLinks("/actual{?flag}");

    mockExpansionStrategyWithVariables(ImmutableMap.of("flag", true));

    List<HalDifference> diffs = findDifferences(expected, actual);

    // the fact that only one link is templated is still considered a difference!
    assertOnlyOneDifference(diffs, MODIFIED, LINK, "/item");

    // but what's important is that with defining those values, it's now possible to continue crawling
    // and comparing, because the link template was expanded to be identical to the resolved link
    assertThat(expected, hasSize(1));
    assertThat(expected.get(0).getHref(), equalTo("/expected?flag=true"));

    assertThat(actual, hasSize(1));
    assertThat(actual.get(0).getHref(), equalTo("/actual?flag=true"));
  }

  @Test
  public void variables_not_present_in_map_should_be_removed() throws Exception {

    List<Link> expected = createLinks("/expected{?a,b}");
    List<Link> actual = createLinks("/actual{?a,b}");

    mockExpansionStrategyWithVariables(
        ImmutableMap.of("a", "A1"));

    findDifferences(expected, actual);

    assertThat(expected, hasSize(1));
    assertThat(expected.get(0).getHref(), equalTo("/expected?a=A1"));

    assertThat(actual, hasSize(1));
    assertThat(actual.get(0).getHref(), equalTo("/actual?a=A1"));
  }
}
