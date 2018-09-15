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

import static io.wcm.caravan.hal.comparison.testing.SameHalResourceMatcher.sameHal;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.COLLECTION;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.ITEM;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.RELATED;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.SECTION;
import static io.wcm.caravan.hal.comparison.testing.StandardRelations.SELF;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.context.HalPathImpl;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessing;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingImpl;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingStep;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessing;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessingImpl;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessingStep;
import io.wcm.caravan.hal.comparison.impl.properties.PropertyProcessing;
import io.wcm.caravan.hal.comparison.testing.TestHalComparisonStrategy;
import io.wcm.caravan.hal.comparison.testing.processing.IgnoreAllEmbeddedResources;
import io.wcm.caravan.hal.comparison.testing.processing.IgnoreAllLinkedResources;
import io.wcm.caravan.hal.comparison.testing.processing.ReportAllEmbeddedResources;
import io.wcm.caravan.hal.comparison.testing.processing.ReportAllLinkedResources;
import io.wcm.caravan.hal.comparison.testing.resources.TestResource;
import io.wcm.caravan.hal.comparison.testing.resources.TestResourceTree;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;


@RunWith(MockitoJUnitRunner.class)
public class HalComparisonRecursionImplTest {

  private TestResourceTree expected;
  private TestResourceTree actual;

  @Mock
  private PropertyProcessing propertyProcessor;
  private EmbeddedProcessing embeddedProcessing;
  private LinkProcessing linkProcessing;

  private HalComparisonStrategy strategy;
  private HalComparisonRecursionImpl impl;

  @Before
  public void setUp() throws Exception {
    expected = new TestResourceTree();
    actual = new TestResourceTree();

    strategy = new TestHalComparisonStrategy();
  }

  private void mockLinkProcessing(LinkProcessingStep processor) {
    this.linkProcessing = new LinkProcessingImpl(ImmutableList.of(processor));
  }

  private void mockEmbeddedProcessing(EmbeddedProcessingStep processor) {
    this.embeddedProcessing = new EmbeddedProcessingImpl(ImmutableList.of(processor));
  }

  private void mockPropertyComparisonError(TestResource expectedResource, TestResource actualResource) {
    ArgumentCaptor<HalComparisonContextImpl> contextCaptor = ArgumentCaptor.forClass(HalComparisonContextImpl.class);

    when(propertyProcessor.process(contextCaptor.capture(), sameHal(expectedResource.asHalResource()), sameHal(actualResource.asHalResource())))
        .thenAnswer(new Answer<List<HalDifference>>() {

          @Override
          public List<HalDifference> answer(InvocationOnMock invocation) throws Throwable {
            HalComparisonContextImpl context = contextCaptor.getValue();
            HalDifferenceImpl result = new HalDifferenceImpl(context, null, null, "mocked comparison error");
            return ImmutableList.of(result);
          }

        });
  }

  private List<HalDifference> findDifferences() {

    String expectedUrl = expected.getEntryPointUrl();
    String actualUrl = actual.getEntryPointUrl();

    HalComparisonContextImpl context = new HalComparisonContextImpl(new HalPathImpl(), expectedUrl, actualUrl);

    if (embeddedProcessing == null) {
      embeddedProcessing = new EmbeddedProcessingImpl(strategy);
    }

    if (linkProcessing == null) {
      linkProcessing = new LinkProcessingImpl(strategy);
    }

    impl = new HalComparisonRecursionImpl(expected, actual, propertyProcessor, embeddedProcessing, linkProcessing);

    Observable<HalDifference> diffs = impl.compareRecursively(context, expected.getEntryPoint().asHalResource(), actual.getEntryPoint().asHalResource());

    return diffs.toList().toBlocking().single();
  }

  @Test
  public void property_processor_should_be_called_once_for_entry_point() {

    List<HalDifference> diff = findDifferences();

    assertThat(diff, empty());
    verify(propertyProcessor).process(any(), sameHal(expected.getEntryPoint()), sameHal(actual.getEntryPoint()));
    verifyNoMoreInteractions(propertyProcessor);
  }

  @Test
  public void property_processor_should_be_called_once_for_each_embedded_resource() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedSection = expectedEntryPoint.createEmbedded(SECTION);
    TestResource expectedItem1 = expectedSection.createEmbedded(ITEM);
    TestResource expectedItem2 = expectedSection.createEmbedded(ITEM);

    TestResource actualEntryPoint = actual.getEntryPoint();
    TestResource actualSection = actualEntryPoint.createEmbedded(SECTION);
    TestResource actualItem1 = actualSection.createEmbedded(ITEM);
    TestResource actualItem2 = actualSection.createEmbedded(ITEM);

    List<HalDifference> diff = findDifferences();

    assertThat(diff, empty());
    verify(propertyProcessor).process(any(), sameHal(expectedEntryPoint), sameHal(actualEntryPoint));
    verify(propertyProcessor).process(any(), sameHal(expectedSection), sameHal(actualSection));
    verify(propertyProcessor).process(any(), sameHal(expectedItem1), sameHal(actualItem1));
    verify(propertyProcessor).process(any(), sameHal(expectedItem2), sameHal(actualItem2));
    verifyNoMoreInteractions(propertyProcessor);
  }

  @Test
  public void property_processor_should_be_called_once_for_each_linked_resource() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedSection = expectedEntryPoint.createLinked(SECTION);
    TestResource expectedItem1 = expectedSection.createLinked(ITEM);
    TestResource expectedItem2 = expectedSection.createLinked(ITEM);

    TestResource actualEntryPoint = actual.getEntryPoint();
    TestResource actualSection = actualEntryPoint.createLinked(SECTION);
    TestResource actualItem1 = actualSection.createLinked(ITEM);
    TestResource actualItem2 = actualSection.createLinked(ITEM);

    List<HalDifference> diff = findDifferences();

    assertThat(diff, empty());
    verify(propertyProcessor).process(any(), sameHal(expectedEntryPoint), sameHal(actualEntryPoint));
    verify(propertyProcessor).process(any(), sameHal(expectedSection), sameHal(actualSection));
    verify(propertyProcessor).process(any(), sameHal(expectedItem1), sameHal(actualItem1));
    verify(propertyProcessor).process(any(), sameHal(expectedItem2), sameHal(actualItem2));
    verifyNoMoreInteractions(propertyProcessor);
  }

  @Test
  public void differences_from_embedded_preprocessing_are_collected() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedItem1 = expectedEntryPoint.createEmbedded(ITEM);
    TestResource expectedItem2 = expectedEntryPoint.createEmbedded(ITEM);
    TestResource expectedRelated1 = expectedEntryPoint.createEmbedded(RELATED);

    TestResource actualEntryPoint = actual.getEntryPoint();
    TestResource actualItem1 = actualEntryPoint.createEmbedded(ITEM);
    TestResource actualItem2 = actualEntryPoint.createEmbedded(ITEM);
    TestResource actualRelated1 = actualEntryPoint.createEmbedded(RELATED);

    // replace embedded processing steps with a single mock that reports all resources as different
    ReportAllEmbeddedResources reportAll = new ReportAllEmbeddedResources();
    mockEmbeddedProcessing(reportAll);

    List<HalDifference> diff = findDifferences();

    // the list of results should contain only the entries created by the mock processor
    assertThat(diff, hasSize(3));
    assertEquals(reportAll.getReportedDiffs().get(0), diff.get(0));
    assertEquals(reportAll.getReportedDiffs().get(1), diff.get(1));
    assertEquals(reportAll.getReportedDiffs().get(2), diff.get(2));

    // but the property processor should still have been called (since the mock processor didn't remove the resources from the lists)
    verify(propertyProcessor).process(any(), sameHal(expectedEntryPoint), sameHal(actualEntryPoint));
    verify(propertyProcessor).process(any(), sameHal(expectedItem1), sameHal(actualItem1));
    verify(propertyProcessor).process(any(), sameHal(expectedItem2), sameHal(actualItem2));
    verify(propertyProcessor).process(any(), sameHal(expectedRelated1), sameHal(actualRelated1));
    verifyNoMoreInteractions(propertyProcessor);
  }

  @Test
  public void differences_from_link_preprocessing_are_collected() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedItem1 = expectedEntryPoint.createLinked(ITEM);
    TestResource expectedItem2 = expectedEntryPoint.createLinked(ITEM);
    TestResource expectedRelated1 = expectedEntryPoint.createLinked(RELATED);

    TestResource actualEntryPoint = actual.getEntryPoint();
    TestResource actualItem1 = actualEntryPoint.createLinked(ITEM);
    TestResource actualItem2 = actualEntryPoint.createLinked(ITEM);
    TestResource actualRelated1 = actualEntryPoint.createLinked(RELATED);

    // replace link processing steps with a single mock that reports  all resources as different
    ReportAllLinkedResources reportAll = new ReportAllLinkedResources();
    mockLinkProcessing(reportAll);

    List<HalDifference> diff = findDifferences();

    // the list of results should contain only the entries created by the mock processor
    assertThat(diff, hasSize(3));
    assertEquals(reportAll.getReportedDiffs().get(0), diff.get(0));
    assertEquals(reportAll.getReportedDiffs().get(1), diff.get(1));
    assertEquals(reportAll.getReportedDiffs().get(2), diff.get(2));

    // but the property processor should still have been called (since the mock processor didn't remove the resources from the lists)
    verify(propertyProcessor).process(any(), sameHal(expectedEntryPoint), sameHal(actualEntryPoint));
    verify(propertyProcessor).process(any(), sameHal(expectedItem1), sameHal(actualItem1));
    verify(propertyProcessor).process(any(), sameHal(expectedItem2), sameHal(actualItem2));
    verify(propertyProcessor).process(any(), sameHal(expectedRelated1), sameHal(actualRelated1));
    verifyNoMoreInteractions(propertyProcessor);
  }

  @Test
  public void differences_in_embedded_properties_should_be_detected_but_not_stop_further_processing() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedItem1 = expectedEntryPoint.createEmbedded(ITEM);
    TestResource expectedItem2 = expectedEntryPoint.createEmbedded(ITEM);
    TestResource expectedItem3 = expectedEntryPoint.createEmbedded(ITEM);

    TestResource actualEntryPoint = actual.getEntryPoint();
    TestResource actualItem1 = actualEntryPoint.createEmbedded(ITEM);
    TestResource actualItem2 = actualEntryPoint.createEmbedded(ITEM);
    TestResource actualItem3 = actualEntryPoint.createEmbedded(ITEM);

    mockPropertyComparisonError(expectedItem2, actualItem2);

    List<HalDifference> diff = findDifferences();

    assertThat(diff, hasSize(1));
    assertEquals("/item[1]", diff.get(0).getHalContext().toString());

    verify(propertyProcessor).process(any(), sameHal(expectedEntryPoint), sameHal(actualEntryPoint));
    verify(propertyProcessor).process(any(), sameHal(expectedItem1), sameHal(actualItem1));
    verify(propertyProcessor).process(any(), sameHal(expectedItem2), sameHal(actualItem2));
    verify(propertyProcessor).process(any(), sameHal(expectedItem3), sameHal(actualItem3));
    verifyNoMoreInteractions(propertyProcessor);
  }

  @Test
  public void differences_in_linked_properties_should_be_detected_but_not_stop_further_processing() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedItem1 = expectedEntryPoint.createLinked(ITEM);
    TestResource expectedItem2 = expectedEntryPoint.createLinked(ITEM);
    TestResource expectedItem3 = expectedEntryPoint.createLinked(ITEM);

    TestResource actualEntryPoint = actual.getEntryPoint();
    TestResource actualItem1 = actualEntryPoint.createLinked(ITEM);
    TestResource actualItem2 = actualEntryPoint.createLinked(ITEM);
    TestResource actualItem3 = actualEntryPoint.createLinked(ITEM);

    mockPropertyComparisonError(expectedItem2, actualItem2);

    List<HalDifference> diff = findDifferences();

    assertThat(diff, hasSize(1));
    assertEquals("/item[1]", diff.get(0).getHalContext().toString());

    verify(propertyProcessor).process(any(), sameHal(expectedEntryPoint), sameHal(actualEntryPoint));
    verify(propertyProcessor).process(any(), sameHal(expectedItem1), sameHal(actualItem1));
    verify(propertyProcessor).process(any(), sameHal(expectedItem2), sameHal(actualItem2));
    verify(propertyProcessor).process(any(), sameHal(expectedItem3), sameHal(actualItem3));
    verifyNoMoreInteractions(propertyProcessor);
  }

  @Test
  public void filtered_embedded_resources_shouldnt_be_compared() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedItem = expectedEntryPoint.createEmbedded(ITEM);

    TestResource actualEntryPoint = actual.getEntryPoint();
    TestResource actualItem = actualEntryPoint.createEmbedded(ITEM);

    mockEmbeddedProcessing(new IgnoreAllEmbeddedResources());

    List<HalDifference> diff = findDifferences();

    assertThat(diff, empty());
    verify(propertyProcessor).process(any(), sameHal(expectedEntryPoint), sameHal(actualEntryPoint));
    verify(propertyProcessor, never()).process(any(), sameHal(expectedItem), sameHal(actualItem));
    verifyNoMoreInteractions(propertyProcessor);
  }

  @Test
  public void filtered_linked_resources_shouldnt_be_loaded() throws Exception {

    // add the links directly, not through the ResourceMocker, so that following the link would lead to an error
    TestResource expectedEntryPoint = expected.getEntryPoint();
    expectedEntryPoint.asHalResource().addLinks(ITEM, new Link("/foo"));

    TestResource actualEntryPoint = actual.getEntryPoint();
    actualEntryPoint.asHalResource().addLinks(ITEM, new Link("/bar"));

    mockLinkProcessing(new IgnoreAllLinkedResources());

    List<HalDifference> diff = findDifferences();

    assertThat(diff, empty());
  }

  @Test
  public void linked_resources_that_have_already_been_processed_should_be_ignored() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedSection = expectedEntryPoint.createLinked(SECTION);
    TestResource expectedItem = expectedSection.createLinked(ITEM);
    expectedItem.addLinkTo(COLLECTION, expectedSection);

    TestResource actualEntryPoint = actual.getEntryPoint();
    TestResource actualSection = actualEntryPoint.createLinked(SECTION);
    TestResource actualItem = actualSection.createLinked(ITEM);
    actualItem.addLinkTo(COLLECTION, actualSection);

    mockPropertyComparisonError(expectedSection, actualSection);

    List<HalDifference> diff = findDifferences();

    // there would be more than on error if the "related" link that points back to the section was followed
    assertThat(diff, hasSize(1));
    assertEquals("/section", diff.get(0).getHalContext().toString());
  }

  @Test
  public void link_processing_steps_are_executed_with_correct_parameters() throws Exception {

    TestResource expectedItem1 = expected.createLinked(ITEM);
    TestResource expectedItem2 = expected.createLinked(ITEM);

    TestResource actualItem1 = actual.createLinked(ITEM);
    TestResource actualItem2 = actual.createLinked(ITEM);

    AtomicInteger stepCalledForItem = new AtomicInteger();

    mockLinkProcessing(new LinkProcessingStep() {

      @Override
      public List<HalDifference> apply(HalComparisonContext context, String relation, List<Link> expectedLinks, List<Link> actualLinks) {

        if (SELF.equals(context.getLastRelation())) {
          assertThat(context.getLastRelation(), equalTo(relation));

          assertThat(expectedLinks, hasSize(1));
          assertThat(expectedLinks.get(0).getHref(), equalTo(context.getExpectedUrl()));

          assertThat(actualLinks, hasSize(1));
          assertThat(actualLinks.get(0).getHref(), equalTo(context.getActualUrl()));
        }
        else {
          stepCalledForItem.incrementAndGet();

          assertThat(context.getLastRelation(), equalTo(ITEM));
          assertThat(context.getLastRelation(), equalTo(relation));

          assertThat(expectedLinks, hasSize(2));
          assertThat(expectedLinks.get(0).getHref(), equalTo(expectedItem1.getUrl()));
          assertThat(expectedLinks.get(1).getHref(), equalTo(expectedItem2.getUrl()));

          assertThat(actualLinks, hasSize(2));
          assertThat(actualLinks.get(0).getHref(), equalTo(actualItem1.getUrl()));
          assertThat(actualLinks.get(1).getHref(), equalTo(actualItem2.getUrl()));
        }

        return Collections.emptyList();
      }
    });

    findDifferences();

    assertThat(stepCalledForItem.get(), equalTo(1));
  }

  @Test
  public void embedded_processing_steps_are_executed_with_correct_parameters() throws Exception {

    TestResource expectedItem1 = expected.createEmbedded(ITEM);
    TestResource expectedItem2 = expected.createEmbedded(ITEM);

    TestResource actualItem1 = actual.createEmbedded(ITEM);
    TestResource actualItem2 = actual.createEmbedded(ITEM);

    AtomicInteger stepCalledForItem = new AtomicInteger();

    mockEmbeddedProcessing(new EmbeddedProcessingStep() {

      @Override
      public List<HalDifference> apply(HalComparisonContext context, String relation, List<HalResource> expectedEmbedded, List<HalResource> actualEmbedded) {
        stepCalledForItem.incrementAndGet();

        assertThat(context.getLastRelation(), equalTo(ITEM));
        assertThat(context.getLastRelation(), equalTo(relation));

        assertThat(expectedEmbedded, hasSize(2));
        assertThat(expectedEmbedded.get(0).getModel(), equalTo(expectedItem1.getJson()));
        assertThat(expectedEmbedded.get(1).getModel(), equalTo(expectedItem2.getJson()));

        assertThat(actualEmbedded, hasSize(2));
        assertThat(actualEmbedded.get(0).getModel(), equalTo(actualItem1.getJson()));
        assertThat(actualEmbedded.get(1).getModel(), equalTo(actualItem2.getJson()));

        return Collections.emptyList();
      }
    });

    findDifferences();

    assertThat(stepCalledForItem.get(), equalTo(1));
  }

  @Test
  public void erorrs_when_resolving_resources_should_be_wrapped() throws Exception {

    TestResource expectedEntryPoint = expected.getEntryPoint();
    TestResource expectedSection = expectedEntryPoint.createLinked(SECTION);
    TestResource expectedItem = expectedSection.createLinked(ITEM);

    // re-create the same structure (otherwise the links wouldn't be followed)
    actual.getEntryPoint().createLinked(SECTION).createLinked(ITEM);

    expected.throwExceptionWhenResolving(expectedItem);

    try {
      findDifferences();
      fail("expected observable to fail with an " + HalComparisonException.class.getSimpleName());
    }
    catch (HalComparisonException ex) {
      assertEquals("/section/item", ex.getHalContext().toString());
      assertEquals(expectedSection.getUrl(), ex.getResourceUrl());
    }

  }
}
