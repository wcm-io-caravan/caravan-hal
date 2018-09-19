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
package io.wcm.caravan.hal.comparison.impl.properties;

import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.ADDITIONAL;
import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.MISSING;
import static io.wcm.caravan.hal.comparison.HalDifference.ChangeType.MODIFIED;
import static io.wcm.caravan.hal.comparison.HalDifference.EntityType.PROPERTY;
import static io.wcm.caravan.hal.comparison.testing.HalDifferenceAssertions.assertDifference;
import static io.wcm.caravan.hal.comparison.testing.HalDifferenceAssertions.assertOnlyOneDifference;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.testing.TestHalComparisonContext;
import io.wcm.caravan.hal.resource.HalResource;


public class PropertyDiffDetectorTest {

  private PropertyDiffDetector processor;
  private HalComparisonContextImpl context;

  @Before
  public void setUp() {
    processor = new PropertyDiffDetector();
    context = new TestHalComparisonContext();
  }

  private List<HalDifference> findDifferences(HalResource expected, HalResource actual) {

    return processor.process(context, expected, actual);
  }

  @Test
  public void no_results_for_equal_resources() throws Exception {

    TestPojo pojo = new TestPojo().withName("foo").withNumber(123);

    HalResource expected = new HalResource(pojo);
    HalResource actual = new HalResource(pojo.copy());

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, empty());
  }

  @Test
  public void no_results_for_equal_resources_with_nested_object() throws Exception {

    TestPojo related = new TestPojo().withName("bar").withNumber(456);
    TestPojo pojo = new TestPojo().withName("foo").withNumber(123).withRelated(related);

    HalResource expected = new HalResource(pojo);
    HalResource actual = new HalResource(pojo.copy());

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, empty());
  }

  @Test
  public void single_global_result_if_all_properties_are_different() throws Exception {

    HalResource expected = new HalResource(new TestPojo().withName("foo").withNumber(123).withFlag(true));
    HalResource actual = new HalResource(new TestPojo().withName("bar").withNumber(456).withFlag(false));

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertOnlyOneDifference(diffs, MODIFIED, PROPERTY, "/");

    assertEquals("", diffs.get(0).getHalContext().getLastRelation());
    assertEquals(expected.getModel(), diffs.get(0).getExpectedJson());
    assertEquals(actual.getModel(), diffs.get(0).getActualJson());
  }

  @Test
  public void single_property_result_if_one_property_is_different() throws Exception {

    HalResource expected = new HalResource(new TestPojo().withName("foo").withNumber(123).withFlag(true));
    HalResource actual = new HalResource(new TestPojo().withName("bar").withNumber(123).withFlag(true));

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertOnlyOneDifference(diffs, MODIFIED, PROPERTY, "/$.name");

    assertEquals(expected.getModel().path("name"), diffs.get(0).getExpectedJson());
    assertEquals(actual.getModel().path("name"), diffs.get(0).getActualJson());
  }

  @Test
  public void no_results_for_equal_arrays() throws Exception {

    TestPojo pojo = new TestPojo().withArray("a", "b", "c");

    HalResource expected = new HalResource(pojo);
    HalResource actual = new HalResource(pojo.copy());

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, empty());
  }

  @Test
  public void single_result_for_arrays_with_one_different_item() throws Exception {

    HalResource expected = new HalResource(new TestPojo().withArray("a", "b", "c"));
    HalResource actual = new HalResource(new TestPojo().withArray("a", "1", "c"));

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertOnlyOneDifference(diffs, MODIFIED, PROPERTY, "/$.array[1]");
  }

  @Test
  public void multiple_result_for_arrays_with_two_different_item() throws Exception {

    HalResource expected = new HalResource(new TestPojo().withArray("a", "b", "c"));
    HalResource actual = new HalResource(new TestPojo().withArray("a", "1", "2"));

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertThat(diffs, hasSize(2));
    assertDifference(diffs.get(0), MODIFIED, PROPERTY, "/$.array[1]");
    assertDifference(diffs.get(1), MODIFIED, PROPERTY, "/$.array[2]");
  }

  @Test
  public void single_result_for_arrays_with_one_missing_item() throws Exception {

    HalResource expected = new HalResource(new TestPojo().withArray("a", "b", "c"));
    HalResource actual = new HalResource(new TestPojo().withArray("a", "b"));

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertOnlyOneDifference(diffs, MISSING, PROPERTY, "/$.array");
  }

  @Test
  public void single_result_for_arrays_with_one_additional_item() throws Exception {

    HalResource expected = new HalResource(new TestPojo().withArray("a", "b"));
    HalResource actual = new HalResource(new TestPojo().withArray("a", "b", "c"));

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertOnlyOneDifference(diffs, ADDITIONAL, PROPERTY, "/$.array");
  }

  @Test
  public void multiple_result_for_arrays_with_one_missing_and_one_different_item() throws Exception {

    HalResource expected = new HalResource(new TestPojo().withArray("a", "b", "c"));
    HalResource actual = new HalResource(new TestPojo().withArray("a", "1"));

    List<HalDifference> diffs = findDifferences(expected, actual);
    assertThat(diffs, hasSize(2));

    assertDifference(diffs.get(0), MISSING, PROPERTY, "/$.array");
    assertDifference(diffs.get(1), MODIFIED, PROPERTY, "/$.array[1]");
  }

  @Test
  public void single_result_for_property_of_different_type() throws Exception {

    TestPojo pojo = new TestPojo().withName("foo").withNumber(123);

    HalResource expected = new HalResource(pojo);
    ObjectNode modifiedClone = expected.getModel().deepCopy().put("name", 123);
    HalResource actual = new HalResource(modifiedClone);

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertOnlyOneDifference(diffs, MODIFIED, PROPERTY, "/$.name");
  }

  @Test
  public void single_result_for_missing_property() throws Exception {

    TestPojo pojo = new TestPojo().withName("foo").withNumber(123);

    HalResource expected = new HalResource(pojo);
    ObjectNode modifiedClone = expected.getModel().deepCopy();
    modifiedClone.remove("name");
    HalResource actual = new HalResource(modifiedClone);

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertOnlyOneDifference(diffs, MISSING, PROPERTY, "/$.name");
  }

  @Test
  public void single_result_for_additional_property() throws Exception {

    TestPojo pojo = new TestPojo().withName("foo").withNumber(123);

    HalResource expected = new HalResource(pojo);
    ObjectNode modifiedClone = expected.getModel().deepCopy();
    modifiedClone.put("added", "value");
    HalResource actual = new HalResource(modifiedClone);

    List<HalDifference> diffs = findDifferences(expected, actual);

    assertOnlyOneDifference(diffs, ADDITIONAL, PROPERTY, "/$.added");
  }

  public static class TestPojo {

    public boolean flag;
    public String name;
    public int number;

    public TestPojo related;

    public List<String> array;

    public TestPojo() {

    }

    TestPojo withFlag(boolean value) {
      this.flag = value;
      return this;
    }

    TestPojo withName(String value) {
      this.name = value;
      return this;
    }

    TestPojo withNumber(int value) {
      this.number = value;
      return this;
    }

    TestPojo withRelated(TestPojo value) {
      this.related = value;
      return this;
    }

    TestPojo withArray(String... values) {
      this.array = values != null ? Arrays.asList(values) : null;
      return this;
    }

    TestPojo copy() {
      return new TestPojo()
          .withFlag(flag)
          .withName(name)
          .withNumber(number)
          .withRelated(related)
          .withArray(array != null ? array.toArray(new String[0]) : null);
    }
  }
}
