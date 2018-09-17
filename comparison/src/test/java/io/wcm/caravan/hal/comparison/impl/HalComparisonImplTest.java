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
package io.wcm.caravan.hal.comparison.impl;

import static io.wcm.caravan.hal.comparison.testing.StandardRelations.ITEM;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.SECTION;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.HalDifference.ChangeType;
import io.wcm.caravan.hal.comparison.HalDifference.EntityType;
import io.wcm.caravan.hal.comparison.testing.TestHalComparisonStrategy;
import io.wcm.caravan.hal.comparison.testing.resources.TestResource;
import io.wcm.caravan.hal.comparison.testing.resources.TestResourceTree;
import rx.Observable;


public class HalComparisonImplTest {

  private TestResourceTree expected;
  private TestResourceTree actual;

  private HalComparisonStrategy strategy;
  private HalComparisonImpl comparison;

  @Before
  public void setUp() throws Exception {
    expected = new TestResourceTree();
    actual = new TestResourceTree();


    comparison = new HalComparisonImpl();
  }

  private List<HalDifference> findDifferences() {

    // use the default config unless a test case has created a specific strategy instance
    if (strategy == null) {
      strategy = new HalComparisonStrategy() {
        // only use the default implementations from the interface
      };
    }
    Observable<HalDifference> diffs = comparison.compare(expected, actual, strategy);

    return diffs.toList().toBlocking().single();
  }

  private HalDifference compareAndAssertThatDifferenceIs(HalDifference.ChangeType changeType, HalDifference.EntityType entityType, String halPath) {

    List<HalDifference> diff = findDifferences();

    assertThat(diff, hasSize(1));
    assertThat(diff.get(0).getChangeType(), equalTo(changeType));
    assertThat(diff.get(0).getEntityType(), equalTo(entityType));
    assertThat(diff.get(0).getHalContext().toString(), equalTo(halPath));

    return diff.get(0);
  }

  @Test
  public void different_entrypoint_should_be_detected() throws Exception {

    expected.getEntryPoint().setText("foo");

    actual.getEntryPoint().setText("bar");

    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/$.text");
  }

  @Test
  public void different_embedded_resource_should_be_detected() throws Exception {

    expected.createEmbedded(ITEM).setNumber(123);

    actual.createEmbedded(ITEM).setNumber(456);

    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/item$.number");
  }

  @Test
  public void missing_embedded_resource_should_be_detected() throws Exception {

    expected.createEmbedded(ITEM);
    expected.createEmbedded(ITEM);

    actual.createEmbedded(ITEM);

    compareAndAssertThatDifferenceIs(ChangeType.MISSING, EntityType.EMBEDDED, "/item");
  }

  @Test
  public void additional_embedded_resource_should_be_detected() throws Exception {

    expected.createEmbedded(ITEM);

    actual.createEmbedded(ITEM);
    actual.createEmbedded(ITEM);

    compareAndAssertThatDifferenceIs(ChangeType.ADDITIONAL, EntityType.EMBEDDED, "/item");
  }

  @Test
  public void different_nameless_linked_resource_should_be_detected() throws Exception {

    expected.createLinked(ITEM).setText("abc");

    actual.createLinked(ITEM).setText("def");

    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/item$.text");
  }

  @Test
  public void missing_nameless_linked_resource_should_be_detected() throws Exception {

    expected.createLinked(ITEM);
    expected.createLinked(ITEM);

    actual.createLinked(ITEM);

    compareAndAssertThatDifferenceIs(ChangeType.MISSING, EntityType.LINK, "/item");
  }

  @Test
  public void additional_nameless_linked_resource_should_be_detected() throws Exception {

    expected.createLinked(ITEM);

    actual.createLinked(ITEM);
    actual.createLinked(ITEM);

    compareAndAssertThatDifferenceIs(ChangeType.ADDITIONAL, EntityType.LINK, "/item");
  }

  @Test
  public void different_named_linked_resource_should_be_detected() throws Exception {

    expected.createLinked(ITEM, "common").setFlag(true);

    actual.createLinked(ITEM, "common").setFlag(false);

    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/item['common']$.flag");
  }

  @Test
  public void missing_named_linked_resource_at_beginning_should_be_detected() throws Exception {

    expected.createLinked(ITEM, "removed");
    expected.createLinked(ITEM, "common");

    actual.createLinked(ITEM, "common");

    List<HalDifference> diff = findDifferences();

    assertThat(diff, hasSize(2));
    assertEquals("/item", diff.get(0).getHalContext().toString()); // first diff only says the number of link has changed
    assertEquals("/item", diff.get(1).getHalContext().toString()); // second diff says exactly which link has been removed
  }

  @Test
  public void additional_named_linked_resource_at_beginning_should_be_detected() throws Exception {

    expected.createLinked(ITEM, "common");

    actual.createLinked(ITEM, "added");
    actual.createLinked(ITEM, "common");

    List<HalDifference> diff = findDifferences();

    assertThat(diff, hasSize(2));
    assertEquals("/item", diff.get(0).getHalContext().toString()); // first diff only says the number of link has changed
    assertEquals("/item", diff.get(1).getHalContext().toString()); // second diff says exactly which link has been added
  }

  @Test
  public void expected_and_actual_url_for_entry_point_should_be_set() throws Exception {

    expected.getEntryPoint().setText("foo");

    actual.getEntryPoint().setText("bar");

    HalDifference diff = compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/$.text");

    assertEquals(expected.getEntryPointUrl(), diff.getHalContext().getExpectedUrl());
    assertEquals(actual.getEntryPointUrl(), diff.getHalContext().getActualUrl());
  }

  @Test
  public void expected_and_actual_url_for_linked_resources_should_point_to_linked_resource() throws Exception {

    TestResource expectedSection = expected.createLinked(SECTION);
    TestResource expectedItem = expectedSection.createLinked(ITEM).setText("foo");

    TestResource actualSection = actual.createLinked(SECTION);
    TestResource actualItem = actualSection.createLinked(ITEM).setText("bar");

    HalDifference diff = compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/section/item$.text");

    assertEquals(expectedItem.getUrl(), diff.getHalContext().getExpectedUrl());
    assertEquals(actualItem.getUrl(), diff.getHalContext().getActualUrl());
  }

  @Test
  public void expected_and_actual_url_for_embedded_resources_should_point_to_context_resource() throws Exception {

    TestResource expectedSection = expected.createLinked(SECTION);
    expectedSection.createEmbedded(ITEM).setText("foo");

    TestResource actualSection = actual.createLinked(SECTION);
    actualSection.createEmbedded(ITEM).setText("bar");

    HalDifference diff = compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/section/item$.text");

    assertEquals(expectedSection.getUrl(), diff.getHalContext().getExpectedUrl());
    assertEquals(actualSection.getUrl(), diff.getHalContext().getActualUrl());
  }

  @Test
  public void configuration_for_ignored_embedded_relations_should_be_respected() throws Exception {

    expected.getEntryPoint()
        .createEmbedded(SECTION).setNumber(123)
        .createEmbedded(ITEM).setText("foo");

    actual.getEntryPoint()
        .createEmbedded(SECTION).setNumber(456)
        .createEmbedded(ITEM).setText("bar");

    strategy = new TestHalComparisonStrategy().addEmbeddedRelationToIgnore(ITEM);

    // only the difference for the section should be reported, not the one for the item
    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/section$.number");
  }

  @Test
  public void configuration_for_ignored_link_relations_should_be_respected() throws Exception {

    expected.getEntryPoint()
        .createLinked(SECTION).setNumber(123)
        .createLinked(ITEM).setText("foo");

    actual.getEntryPoint()
        .createLinked(SECTION).setNumber(456)
        .createLinked(ITEM).setText("bar");

    strategy = new TestHalComparisonStrategy().addLinkRelationToIgnore(ITEM);

    // only the difference for the section should be reported, not the one for the item
    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/section$.number");
  }

  @Test
  public void indices_should_be_included_in_halpath_for_multiple_embedded_resources_with_same_relation() {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    expectedEntryPoint.createEmbedded(ITEM).setText("foo");
    expectedEntryPoint.createEmbedded(ITEM).setText("foo");
    expectedEntryPoint.createEmbedded(ITEM).setText("foo");

    TestResource actualEntryPoint = actual.getEntryPoint();
    actualEntryPoint.createEmbedded(ITEM).setText("foo");
    actualEntryPoint.createEmbedded(ITEM).setText("bar");
    actualEntryPoint.createEmbedded(ITEM).setText("foo");

    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/item[1]$.text");
  }

  @Test
  public void indices_should_be_included_in_halpath_for_multiple_unnamed_linked_resources_with_same_relation() {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    expectedEntryPoint.createLinked(ITEM).setText("foo");
    expectedEntryPoint.createLinked(ITEM).setText("foo");
    expectedEntryPoint.createLinked(ITEM).setText("foo");

    TestResource actualEntryPoint = actual.getEntryPoint();
    actualEntryPoint.createLinked(ITEM).setText("foo");
    actualEntryPoint.createLinked(ITEM).setText("bar");
    actualEntryPoint.createLinked(ITEM).setText("foo");

    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/item[1]$.text");
  }

  @Test
  public void indices_should_be_included_in_halpath_for_multiple_named_linked_resources_with_same_relation() {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    expectedEntryPoint.createLinked(ITEM, "name1").setText("foo");
    expectedEntryPoint.createLinked(ITEM, "name2").setText("foo");
    expectedEntryPoint.createLinked(ITEM, "name3").setText("foo");

    TestResource actualEntryPoint = actual.getEntryPoint();
    actualEntryPoint.createLinked(ITEM, "name1").setText("foo");
    actualEntryPoint.createLinked(ITEM, "name2").setText("bar");
    actualEntryPoint.createLinked(ITEM, "name3").setText("foo");

    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/item['name2']$.text");
  }

  @Test
  public void indices_should_be_included_in_halpath_for_different_array_values() {

    expected.createEmbedded(ITEM).setArray("a", "b", "c");
    actual.createEmbedded(ITEM).setArray("a", "b", "foo");

    compareAndAssertThatDifferenceIs(ChangeType.MODIFIED, EntityType.PROPERTY, "/item$.array[2]");
  }
}
