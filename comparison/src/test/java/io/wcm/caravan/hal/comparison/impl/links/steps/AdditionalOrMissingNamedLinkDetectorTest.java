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

import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.ADDITIONAL;
import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.MISSING;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.HalDifference.ChangeType;
import io.wcm.caravan.hal.comparison.testing.TestHalComparisonContext;
import io.wcm.caravan.hal.resource.Link;

public class AdditionalOrMissingNamedLinkDetectorTest {

  private AdditionalOrMissingNamedLinkDetector processor;
  private HalComparisonContext context;

  @Before
  public void setUp() {
    processor = new AdditionalOrMissingNamedLinkDetector();
    context = new TestHalComparisonContext();
  }

  private List<Link> createLinks(String... names) {

    return Stream.of(names)
        .map(name -> {
          String randomPath = "/" + RandomStringUtils.random(40);
          return new Link(randomPath).setName(name);
        })
        .collect(Collectors.toList());
  }

  private List<HalDifference> findDifferences(List<Link> expected, List<Link> actual) {
    return processor.apply(context, expected, actual);
  }

  private void verifyResults(List<HalDifference> actualResults, List<Link> expected, List<Link> actual, Map<String, ChangeType> expectedChanges) {

    assertThat(actualResults, hasSize(expectedChanges.size()));

    int resultIndex = 0;
    for (Entry<String, ChangeType> entry : expectedChanges.entrySet()) {
      HalDifference nextResult = actualResults.get(resultIndex++);
      String description = nextResult.getDescription();

      String linkName = entry.getKey();
      assertThat(nextResult.getChangeType(), equalTo(entry.getValue()));
      assertThat(description, containsString(linkName));
    }

    verifyItemsRemoved(expectedChanges, actual, ADDITIONAL);
    verifyItemsRemoved(expectedChanges, expected, MISSING);
  }

  private void verifyItemsRemoved(Map<String, ChangeType> resultMap, List<Link> collection, ChangeType changeType) {

    List<String> linkNames = resultMap.entrySet().stream()
        .filter(entry -> entry.getValue().equals(changeType))
        .map(entry -> entry.getKey())
        .collect(Collectors.toList());

    boolean foundItem = collection.stream()
        .map(Link::getName)
        .anyMatch(n -> linkNames.contains(n));

    assertFalse("One of " + StringUtils.join(linkNames, ", ") + " is still contained in the given list", foundItem);
  }


  @Test
  public void no_results_and_no_exceptions_if_both_lists_are_empty() throws Exception {

    List<Link> expected = createLinks();
    List<Link> actual = createLinks();

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, empty());
  }


  @Test
  public void all_actual_items_are_unexpected_if_expected_list_is_empty() throws Exception {

    List<Link> expected = createLinks();
    List<Link> actual = createLinks("a1", "a2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("a1", ADDITIONAL, "a2", ADDITIONAL));
  }

  @Test
  public void all_expected_items_are_missing_if_actual_list_is_empty() throws Exception {

    List<Link> expected = createLinks("e1", "e2");
    List<Link> actual = createLinks();

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("e1", MISSING, "e2", MISSING));
  }

  @Test
  public void item_added_at_the_beginning_is_detected() throws Exception {

    List<Link> expected = createLinks("c1", "c2");
    List<Link> actual = createLinks("a1", "c1", "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("a1", ADDITIONAL));
  }

  @Test
  public void item_added_in_the_middle_is_detected() throws Exception {

    List<Link> expected = createLinks("c1", "c2");
    List<Link> actual = createLinks("c1", "a1", "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("a1", ADDITIONAL));
  }

  @Test
  public void item_added_at_the_end_is_detected() throws Exception {

    List<Link> expected = createLinks("c1", "c2");
    List<Link> actual = createLinks("c1", "c2", "a1");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("a1", ADDITIONAL));
  }

  @Test
  public void item_removed_at_the_beginning_is_detected() throws Exception {

    List<Link> expected = createLinks("e1", "c1", "c2");
    List<Link> actual = createLinks("c1", "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("e1", MISSING));
  }

  public void item_removed_in_the_middle_is_detected() throws Exception {

    List<Link> expected = createLinks("c1", "e1", "c2");
    List<Link> actual = createLinks("c1", "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("e1", MISSING));
  }

  @Test
  public void item_removed_at_the_end_is_detected() throws Exception {

    List<Link> expected = createLinks("c1", "c2", "e1");
    List<Link> actual = createLinks("c1", "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("e1", MISSING));
  }

  @Test
  public void item_replaced_at_the_beginning_is_detected() throws Exception {

    List<Link> expected = createLinks("e1", "c1", "c2");
    List<Link> actual = createLinks("a1", "c1", "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("e1", MISSING, "a1", ADDITIONAL));
  }

  @Test
  public void item_replaced_in_the_middle_is_detected() throws Exception {

    List<Link> expected = createLinks("c1", "e1", "c2");
    List<Link> actual = createLinks("c1", "a1", "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("e1", MISSING, "a1", ADDITIONAL));
  }

  @Test
  public void item_replaced_at_the_end_is_detected() throws Exception {

    List<Link> expected = createLinks("c1", "c2", "e1");
    List<Link> actual = createLinks("c1", "c2", "a1");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of("e1", MISSING, "a1", ADDITIONAL));
  }

  @Test
  public void no_processing_if_names_are_not_unique_in_expected() throws Exception {
    List<Link> expected = createLinks("foo", "foo");
    List<Link> actual = createLinks("foo", "bar");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of());
  }

  @Test
  public void no_processing_if_names_are_not_unique_in_actual() throws Exception {
    List<Link> expected = createLinks("foo", "bar");
    List<Link> actual = createLinks("foo", "foo");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of());
  }

  @Test
  public void no_processing_if_names_are_null_in_expected() throws Exception {
    List<Link> expected = createLinks("c1", null, "c2");
    List<Link> actual = createLinks("c1", "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of());
  }

  @Test
  public void no_processing_if_names_are_null_in_actual() throws Exception {
    List<Link> expected = createLinks("c1", "c2");
    List<Link> actual = createLinks("c1", null, "c2");

    List<HalDifference> diffs = findDifferences(expected, actual);

    verifyResults(diffs, expected, actual, ImmutableMap.of());
  }
}
