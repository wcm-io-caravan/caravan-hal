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
package io.wcm.caravan.hal.comparison.impl.context;

import static io.wcm.caravan.hal.comparison.testing.StandardRelations.COLLECTION;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.ITEM;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.SECTION;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalComparisonImpl;
import io.wcm.caravan.hal.comparison.testing.resources.TestResource;
import io.wcm.caravan.hal.comparison.testing.resources.TestResourceTree;
import rx.Observable;


public class HalComparisonContextImplTest {

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

  @Test
  public void context_should_keep_track_of_all_parents_of_linked_resources() throws Exception {

    TestResource expectedCollection = expected.createLinked(COLLECTION);
    TestResource expectedSection = expectedCollection.createLinked(SECTION);
    expectedSection.createLinked(ITEM);

    TestResource actualCollection = actual.createLinked(COLLECTION);
    TestResource actualSection = actualCollection.createLinked(SECTION);
    actualSection.createLinked(ITEM);

    strategy = new HalComparisonStrategy() {

      @Override
      public boolean ignoreLinkTo(HalComparisonContext halContext) {

        String relation = halContext.getLastRelation();

        if (COLLECTION.equals(relation)) {
          assertThat(halContext.getAllRelations(), Matchers.contains(COLLECTION));

          assertThat(halContext.getParentResourceWithRelation(COLLECTION), nullValue());
        }
        if (SECTION.equals(relation)) {
          assertThat(halContext.getAllRelations(), Matchers.contains(COLLECTION, SECTION));

          assertThat(halContext.getParentResourceWithRelation(COLLECTION), equalTo(expectedCollection.asHalResource()));
          assertThat(halContext.getParentResourceWithRelation(SECTION), nullValue());
        }
        else if (ITEM.equals(relation)) {
          assertThat(halContext.getAllRelations(), Matchers.contains(COLLECTION, SECTION, ITEM));

          assertThat(halContext.getParentResourceWithRelation(COLLECTION), equalTo(expectedCollection.asHalResource()));
          assertThat(halContext.getParentResourceWithRelation(SECTION), equalTo(expectedSection.asHalResource()));
          assertThat(halContext.getParentResourceWithRelation(ITEM), nullValue());
        }

        return false;
      }

    };

    List<HalDifference> diff = findDifferences();

    assertThat(diff, hasSize(0));
  }

  @Test
  public void context_should_keep_track_of_all_parents_of_embedded_resources() throws Exception {

    TestResource expectedCollection = expected.createEmbedded(COLLECTION);
    TestResource expectedSection = expectedCollection.createEmbedded(SECTION);
    expectedSection.createEmbedded(ITEM);

    TestResource actualCollection = actual.createEmbedded(COLLECTION);
    TestResource actualSection = actualCollection.createEmbedded(SECTION);
    actualSection.createEmbedded(ITEM);

    strategy = new HalComparisonStrategy() {

      @Override
      public boolean ignoreEmbeddedAt(HalComparisonContext halContext) {

        String relation = halContext.getLastRelation();

        if (COLLECTION.equals(relation)) {
          assertThat(halContext.getAllRelations(), Matchers.contains(COLLECTION));

          assertThat(halContext.getParentResourceWithRelation(COLLECTION), nullValue());
        }
        else if (SECTION.equals(relation)) {
          assertThat(halContext.getAllRelations(), Matchers.contains(COLLECTION, SECTION));

          // since for embedded resources there is no guarantee that you will find the same HalResource instance
          // we have to compare the underlying JsonNode instead
          assertThat(halContext.getParentResourceWithRelation(COLLECTION).getModel(), equalTo(expectedCollection.getJson()));
          assertThat(halContext.getParentResourceWithRelation(SECTION), nullValue());
        }
        else if (ITEM.equals(relation)) {
          assertThat(halContext.getAllRelations(), Matchers.contains(COLLECTION, SECTION, ITEM));

          assertThat(halContext.getParentResourceWithRelation(COLLECTION).getModel(), equalTo(expectedCollection.getJson()));
          assertThat(halContext.getParentResourceWithRelation(SECTION).getModel(), equalTo(expectedSection.getJson()));
          assertThat(halContext.getParentResourceWithRelation(ITEM), nullValue());
        }

        return false;
      }

    };

    List<HalDifference> diff = findDifferences();

    assertThat(diff, hasSize(0));
  }


  @Test
  public void getParentResourceWithRelation_should_return_the_nearest_parent() throws Exception {

    TestResource expectedItem1 = expected.createLinked(ITEM);
    TestResource expectedItem2 = expectedItem1.createLinked(ITEM);
    TestResource expectedItem3 = expectedItem2.createLinked(ITEM);
    expectedItem3.createLinked(ITEM);

    TestResource actualItem1 = actual.createLinked(ITEM);
    TestResource actualItem2 = actualItem1.createLinked(ITEM);
    TestResource actualItem3 = actualItem2.createLinked(ITEM);
    actualItem3.createLinked(ITEM);

    strategy = new HalComparisonStrategy() {

      @Override
      public boolean ignoreLinkTo(HalComparisonContext halContext) {

        int itemDepth = halContext.getAllRelations().size();

        if (halContext.getLastRelation().equals(ITEM)) {
          if (itemDepth == 1) {
            // this is the first item link that hasn't been followed, so there is no parent "item2 resource
            assertThat(halContext.getParentResourceWithRelation(ITEM), nullValue());
          }
          else if (itemDepth == 2) {
            assertThat(halContext.getAllRelations(), Matchers.contains(ITEM, ITEM));

            assertThat(halContext.getParentResourceWithRelation(ITEM), equalTo(expectedItem1.asHalResource()));
          }
          else if (itemDepth == 3) {
            assertThat(halContext.getAllRelations(), Matchers.contains(ITEM, ITEM, ITEM));

            assertThat(halContext.getParentResourceWithRelation(ITEM), equalTo(expectedItem2.asHalResource()));
          }
          else if (itemDepth == 4) {
            assertThat(halContext.getAllRelations(), Matchers.contains(ITEM, ITEM, ITEM, ITEM));

            assertThat(halContext.getParentResourceWithRelation(ITEM), equalTo(expectedItem3.asHalResource()));
          }
        }

        return false;
      }

    };

    List<HalDifference> diff = findDifferences();

    assertThat(diff, hasSize(0));
  }
}
