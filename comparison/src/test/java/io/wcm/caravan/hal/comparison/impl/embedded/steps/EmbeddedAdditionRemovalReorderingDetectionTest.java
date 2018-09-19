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

import static com.google.common.collect.Lists.newArrayList;
import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.ADDITIONAL;
import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.MISSING;
import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.REORDERED;
import static io.wcm.caravan.hal.comparison.HalDifference.EntityType.EMBEDDED;
import static io.wcm.caravan.hal.comparison.testing.HalDifferenceAssertions.assertDifference;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.ITEM;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.testing.TestHalComparisonContext;
import io.wcm.caravan.hal.resource.HalResource;


public class EmbeddedAdditionRemovalReorderingDetectionTest {

  private HalComparisonStrategy strategy;

  private int idCounter;

  @Before
  public void setUp() {
    idCounter = 0;
  }

  private void mockExpansionStrategyWithFunction(Function<HalResource, String> idProvider) {
    strategy = new HalComparisonStrategy() {

      @Override
      public Function<HalResource, String> getIdProvider(HalComparisonContext context) {
        return idProvider;
      }
    };
  }

  private static String getIdProperty(HalResource hal) {
    return hal.getModel().path("id").asText();
  }

  private List<HalDifference> findDifferences(List<HalResource> expected, List<HalResource> actual) {

    // use the default strategy unless a specific strategy was defined in the test
    if (strategy == null) {
      mockExpansionStrategyWithFunction(EmbeddedAdditionRemovalReorderingDetectionTest::getIdProperty);
    }

    EmbeddedAdditionRemovalReorderingDetection processor = new EmbeddedAdditionRemovalReorderingDetection(strategy);

    // the LinkProcessingImpl will already add the relation to the context before calling its steps, so we do the same here
    HalComparisonContextImpl context = new TestHalComparisonContext().withAppendedHalPath(ITEM, new HalResource());

    return processor.apply(context, expected, actual);
  }

  private HalResource createUniqueResource() {
    HalResource hal = new HalResource();
    hal.getModel().put("id", idCounter++);
    return hal;
  }

  private List<HalResource> createUniqueResources(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> createUniqueResource())
        .collect(Collectors.toList());
  }

  private List<HalResource> createIdenticalResources(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> new HalResource())
        .collect(Collectors.toList());
  }

  private static List<HalResource> cloneList(Collection<HalResource> other) {
    return other.stream()
        .map(HalResource::getModel)
        .map(ObjectNode::deepCopy)
        .map(HalResource::new)
        .collect(Collectors.toList());
  }

  private static List<HalResource> cloneListAndReverse(Collection<HalResource> other) {
    List<HalResource> cloned = cloneList(other);
    return Lists.reverse(cloned);
  }

  private static List<Integer> getOrdering(Collection<HalResource> resources) {
    return resources.stream()
        .map(hal -> hal.getModel().path("id").asInt())
        .collect(Collectors.toList());
  }

  @Test
  public void should_not_modify_identical_lists() {

    List<HalResource> expected = createUniqueResources(10);
    List<HalResource> actual = cloneList(expected);

    List<Integer> expectedOrdering = getOrdering(expected);

    mockExpansionStrategyWithFunction(new EmbeddedAdditionRemovalReorderingDetection.DefaultIdProvider());
    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, hasSize(0));
    assertThat(getOrdering(expected), equalTo(expectedOrdering));
    assertThat(getOrdering(actual), equalTo(expectedOrdering));
  }

  @Test
  public void should_not_modify_undistinguishable_lists() {

    List<HalResource> expected = createIdenticalResources(100);
    List<HalResource> actual = createIdenticalResources(100);

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, hasSize(0));
  }


  @Test
  public void should_detect_if_the_only_item_has_been_replaced() {

    List<HalResource> expected = createUniqueResources(1);
    List<HalResource> actual = createUniqueResources(1);

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, hasSize(2));
    assertDifference(diffs.get(1), ADDITIONAL, EMBEDDED, "/item[0]");
    assertDifference(diffs.get(0), MISSING, EMBEDDED, "/item[0]");
  }

  @Test
  public void should_detect_if_all_items_were_removed() {

    List<HalResource> expected = createUniqueResources(2);
    List<HalResource> actual = Collections.emptyList();

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, hasSize(2));
    assertDifference(diffs.get(0), MISSING, EMBEDDED, "/item[0]");
    assertDifference(diffs.get(1), MISSING, EMBEDDED, "/item[1]");
  }

  @Test
  public void should_detect_if_all_items_were_added() {

    List<HalResource> expected = Collections.emptyList();
    List<HalResource> actual = createUniqueResources(2);

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, hasSize(2));
    assertDifference(diffs.get(0), ADDITIONAL, EMBEDDED, "/item[0]");
    assertDifference(diffs.get(1), ADDITIONAL, EMBEDDED, "/item[1]");
  }

  @Test
  public void should_detect_that_ordering_is_the_only_difference() {

    List<HalResource> expected = createUniqueResources(10);
    List<HalResource> actual = cloneListAndReverse(expected);

    List<Integer> expectedOrdering = getOrdering(expected);

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, hasSize(1));
    assertThat(getOrdering(expected), equalTo(expectedOrdering));
    assertThat(getOrdering(actual), equalTo(expectedOrdering));
  }

  public void checkDetectionOfAddedItems(boolean reorder, int... insertIndices) {
    List<HalResource> expected = createUniqueResources(10);
    List<HalResource> actual = reorder ? cloneListAndReverse(expected) : cloneList(expected);

    List<HalResource> added = new ArrayList<>();
    for (int i : insertIndices) {
      HalResource addedResource = createUniqueResource();
      actual.add(i, addedResource);
      added.add(addedResource);
    }

    List<Integer> expectedOrdering = getOrdering(expected);
    List<HalDifference> diffs = findDifferences(expected, actual);

    int diffCountBecauseOfReordering = reorder ? 1 : 0;

    assertThat(diffs, hasSize(added.size() + diffCountBecauseOfReordering));

    if (reorder) {
      assertThat(diffs.get(0).getChangeType(), equalTo(REORDERED));
    }
    for (int i = 0; i < added.size(); i++) {
      HalDifference diff = diffs.get(i + diffCountBecauseOfReordering);
      assertThat(diff.getChangeType(), equalTo(ADDITIONAL));
      assertThat(diff.getEntityType(), equalTo(EMBEDDED));
      assertThat(diff.getActualJson(), equalTo(added.get(i).getModel()));
    }

    assertThat(getOrdering(expected), equalTo(expectedOrdering));
    assertThat(getOrdering(actual), equalTo(expectedOrdering));
  }

  @Test
  public void should_find_added_item_at_beginning_when_ordering_hasnt_changed() {
    checkDetectionOfAddedItems(false, 0);
  }

  @Test
  public void should_find_added_item_in_the_middle_when_ordering_hasnt_changed() {
    checkDetectionOfAddedItems(false, 4);
  }

  @Test
  public void should_find_added_item_at_end_when_ordering_hasnt_changed() {
    checkDetectionOfAddedItems(false, 10);
  }

  @Test
  public void should_find_multiple_added_items_when_ordering_hasnt_changed() {
    checkDetectionOfAddedItems(false, 0, 4, 12);
  }

  @Test
  public void should_find_added_item_at_beginning_when_ordering_has_changed() {
    checkDetectionOfAddedItems(true, 0);
  }

  @Test
  public void should_find_added_item_in_the_middle_when_ordering_has_changed() {
    checkDetectionOfAddedItems(true, 4);
  }

  @Test
  public void should_find_added_item_at_end_when_ordering_has_changed() {
    checkDetectionOfAddedItems(true, 10);
  }

  @Test
  public void should_find_multiple_added_items_when_ordering_hat_changed() {
    checkDetectionOfAddedItems(true, 0, 4, 12);
  }

  public void checkDetectionOfRemovedItems(boolean reorder, int... removeIndices) {
    List<HalResource> expected = createUniqueResources(10);
    List<HalResource> actual = reorder ? cloneListAndReverse(expected) : cloneList(expected);

    List<HalResource> removed = new ArrayList<>();
    for (int i : removeIndices) {
      HalResource removedResource = actual.remove(i);
      removed.add(removedResource);
    }

    List<Integer> expectedOrdering = getOrdering(actual);
    expectedOrdering.sort(Ordering.natural());

    List<HalDifference> diffs = findDifferences(expected, actual);

    int diffCountBecauseOfReordering = reorder ? 1 : 0;

    assertThat(diffs, hasSize(removed.size() + diffCountBecauseOfReordering));
    if (reorder) {
      assertThat(diffs.get(0).getChangeType(), equalTo(REORDERED));
    }
    for (int i = 0; i < removed.size(); i++) {
      HalDifference diff = diffs.get(i + diffCountBecauseOfReordering);
      assertThat(diff.getChangeType(), equalTo(MISSING));
      assertThat(diff.getEntityType(), equalTo(EMBEDDED));
      assertThat(diff.getExpectedJson(), equalTo(removed.get(i).getModel()));
    }
    assertThat(getOrdering(expected), equalTo(expectedOrdering));
    assertThat(getOrdering(actual), equalTo(expectedOrdering));
  }


  @Test
  public void should_find_removed_item_at_beginning_when_ordering_hasnt_changed() {
    checkDetectionOfRemovedItems(false, 0);
  }

  @Test
  public void should_find_removed_item_in_the_middle_when_ordering_hasnt_changed() {
    checkDetectionOfRemovedItems(false, 4);
  }

  @Test
  public void should_find_removed_item_at_end_when_ordering_hasnt_changed() {
    checkDetectionOfAddedItems(false, 9);
  }

  @Test
  public void should_find_multiple_removed_items_when_ordering_hasnt_changed() {
    checkDetectionOfAddedItems(false, 0, 3, 7);
  }

  @Test
  public void should_find_removed_item_at_beginning_when_ordering_has_changed() {
    checkDetectionOfRemovedItems(true, 0);
  }

  @Test
  public void should_find_removed_item_in_the_middle_when_ordering_has_changed() {
    checkDetectionOfRemovedItems(true, 4);
  }

  @Test
  public void should_find_removed_item_at_end_when_ordering_has_changed() {
    checkDetectionOfAddedItems(true, 9);
  }

  @Test
  public void should_find_multiple_removed_items_when_ordering_has_changed() {
    checkDetectionOfAddedItems(true, 0, 3, 7);
  }

  @Test
  public void description_should_contain_resource_title_if_available() {

    HalResource hal = new HalResource();
    hal.getModel()
        .put("title", "title text")
        .put("name", "name text");

    List<HalResource> expected = newArrayList(hal);
    List<HalResource> actual = Collections.emptyList();

    List<HalDifference> diffs = findDifferences(expected, actual);

    String desc = diffs.get(0).getDescription();
    assertThat(desc, Matchers.containsString(ITEM));
    assertThat(desc, Matchers.containsString("title text"));
  }

  @Test
  public void description_should_contain_resource_name_if_title_not_available() {

    HalResource hal = new HalResource();
    hal.getModel()
        .put("name", "name text");

    List<HalResource> expected = newArrayList(hal);
    List<HalResource> actual = Collections.emptyList();

    List<HalDifference> diffs = findDifferences(expected, actual);

    String desc = diffs.get(0).getDescription();
    assertThat(desc, Matchers.containsString(ITEM));
    assertThat(desc, Matchers.containsString("name text"));
  }

  @Test
  public void description_should_contain_relation_if_title_and_name_not_available() {

    HalResource hal = new HalResource();

    List<HalResource> expected = newArrayList(hal);
    List<HalResource> actual = Collections.emptyList();

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs.get(0).getDescription(), Matchers.containsString(ITEM));
  }
}
