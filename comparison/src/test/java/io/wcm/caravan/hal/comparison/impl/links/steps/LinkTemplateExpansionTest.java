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

import static com.google.common.collect.Lists.newArrayList;
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
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
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

  private List<HalDifference> applyLinkTemplateProcessor(List<Link> expected, List<Link> actual) {

    // use the default strategy unless a specific strategy was defined in the test
    if (strategy == null) {
      strategy = new TestHalComparisonStrategy();
    }

    LinkTemplateProcessor processor = new LinkTemplateProcessor(strategy);

    // the LinkProcessingImpl will already add the relation to the context before calling its steps, so we do the same here
    HalComparisonContextImpl context = new TestHalComparisonContext().withAppendedHalPath(ITEM, new HalResource());

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

    applyLinkTemplateProcessor(expected, actual);
  }

  @Test
  public void templates_should_be_filtered_instead_of_expanded_if_default_strategy_is_used() throws Exception {

    List<Link> expected = createLinks("/expected{?a,b}");
    List<Link> actual = createLinks("/actual/{a}/{b}");

    applyLinkTemplateProcessor(expected, actual);

    assertThat(expected, hasSize(0));
    assertThat(actual, hasSize(0));
  }

  @Test
  public void templates_should_be_filtered_instead_of_expanded_if_no_variables_are_specified() throws Exception {

    List<Link> expected = createLinks("/expected{?a,b}");
    List<Link> actual = createLinks("/actual/{a}/{b}");

    mockExpansionStrategyWithVariables();

    applyLinkTemplateProcessor(expected, actual);

    assertThat(expected, hasSize(0));
    assertThat(actual, hasSize(0));
  }

  @Test
  public void templates_should_be_filtered_instead_of_expanded_if_strategy_returns_null() throws Exception {

    List<Link> expected = createLinks("/expected{?a,b}");
    List<Link> actual = createLinks("/actual{?a,b}");

    strategy = new TestHalComparisonStrategy() {

      @Override
      public List<Map<String, Object>> getVariablesToExpandLinkTemplate(HalComparisonContext context, Link expectedLink, Link actualLink) {
        return null;
      }

    };

    applyLinkTemplateProcessor(expected, actual);

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

    applyLinkTemplateProcessor(expected, actual);

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

    List<HalDifference> diffs = applyLinkTemplateProcessor(expected, actual);

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

    List<HalDifference> diffs = applyLinkTemplateProcessor(expected, actual);

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

    mockExpansionStrategyWithVariables(ImmutableMap.of("a", "A1"));

    applyLinkTemplateProcessor(expected, actual);

    assertThat(expected, hasSize(1));
    assertThat(expected.get(0).getHref(), equalTo("/expected?a=A1"));

    assertThat(actual, hasSize(1));
    assertThat(actual.get(0).getHref(), equalTo("/actual?a=A1"));
  }

  @Test
  public void parameters_should_be_used_for_expanded_link_names() throws Exception {

    List<Link> expected = createLinks("/expected{?a,b}");
    List<Link> actual = createLinks("/actual{?a,b}");

    Map<String, Object> parameters = ImmutableMap.of("a", "A1", "b", "B1");
    mockExpansionStrategyWithVariables(parameters);

    applyLinkTemplateProcessor(expected, actual);

    assertThat(expected.get(0).getName(), equalTo(parameters.toString()));
    assertThat(actual.get(0).getName(), equalTo(parameters.toString()));
  }

  @Test
  public void parameters_should_be_appended_to_existing_link_names() throws Exception {

    String name = "linkName";

    List<Link> expected = newArrayList(new Link("/expected{?a}").setName(name));
    List<Link> actual = newArrayList(new Link("/actual{?a}").setName(name));

    Map<String, Object> parameters = ImmutableMap.of("a", "A1");
    mockExpansionStrategyWithVariables(parameters);

    applyLinkTemplateProcessor(expected, actual);

    assertThat(expected.get(0).getName(), equalTo(name + parameters.toString()));
    assertThat(actual.get(0).getName(), equalTo(name + parameters.toString()));
  }
}
