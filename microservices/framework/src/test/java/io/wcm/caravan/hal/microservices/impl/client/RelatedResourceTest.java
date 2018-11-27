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
package io.wcm.caravan.hal.microservices.impl.client;

import static io.wcm.caravan.hal.api.annotations.StandardRelations.ALTERNATE;
import static io.wcm.caravan.hal.api.annotations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.hal.microservices.testing.resources.TestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceState;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;


public class RelatedResourceTest {

  private RequestMetricsCollector metrics;
  private BinaryResourceLoader binaryLoader;
  private JsonResourceLoader jsonLoader;
  private TestResource entryPoint;

  @Before
  public void setUp() {
    metrics = RequestMetricsCollector.create();
    binaryLoader = Mockito.mock(BinaryResourceLoader.class);

    TestResourceTree testResourceTree = new TestResourceTree();
    jsonLoader = testResourceTree;
    entryPoint = testResourceTree.getEntryPoint();
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {
    HalApiClientImpl client = new HalApiClientImpl(jsonLoader, binaryLoader, metrics);
    T clientProxy = client.getEntryPoint(entryPoint.getUrl(), halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }


  @HalApiInterface
  interface ResourceWithSingleRelated {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getItem();
  }

  @Test
  public void single_linked_resource_should_be_emitted() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    ResourceWithSingleRelated proxy = createClientProxy(ResourceWithSingleRelated.class);

    TestResourceState linkedState = proxy.getItem()
        .flatMap(ResourceWithSingleState::getProperties).blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test(expected = NoSuchElementException.class)
  public void single_linked_resource_should_fail_if_link_is_not_present() throws Exception {

    entryPoint.createLinked(ALTERNATE).setText("item text");

    ResourceWithSingleRelated proxy = createClientProxy(ResourceWithSingleRelated.class);

    proxy.getItem().blockingGet();
  }

  @Test
  public void single_embedded_resource_should_be_emitted() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    ResourceWithSingleRelated proxy = createClientProxy(ResourceWithSingleRelated.class);

    TestResourceState linkedState = proxy.getItem()
        .flatMap(ResourceWithSingleState::getProperties).blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test(expected = NoSuchElementException.class)
  public void single_embedded_resource_should_fail_if_resource_is_not_present() throws Exception {

    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    ResourceWithSingleRelated proxy = createClientProxy(ResourceWithSingleRelated.class);

    proxy.getItem().blockingGet();
  }


  @HalApiInterface
  interface ResourceWithOptionalRelated {

    @RelatedResource(relation = ITEM)
    Maybe<ResourceWithSingleState> getOptionalItem();
  }

  @Test
  public void maybe_linked_resource_should_be_emitted() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    ResourceWithOptionalRelated proxy = createClientProxy(ResourceWithOptionalRelated.class);

    Maybe<TestResourceState> maybeLinkedState = proxy.getOptionalItem()
        .flatMapSingleElement(ResourceWithSingleState::getProperties);

    TestResourceState linkedState = maybeLinkedState.blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void maybe_linked_resource_should_be_empty_if_link_is_not_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createLinked(ALTERNATE).setText("item text");

    ResourceWithOptionalRelated proxy = createClientProxy(ResourceWithOptionalRelated.class);

    Maybe<ResourceWithSingleState> maybeLinked = proxy.getOptionalItem();

    assertThat(maybeLinked.isEmpty().blockingGet()).isTrue();
  }

  @Test
  public void maybe_embedded_resource_should_be_emitted() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    ResourceWithOptionalRelated proxy = createClientProxy(ResourceWithOptionalRelated.class);

    Maybe<TestResourceState> maybeLinkedState = proxy.getOptionalItem()
        .flatMapSingleElement(ResourceWithSingleState::getProperties);

    TestResourceState linkedState = maybeLinkedState.blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void maybe_embedded_resource_should_be_empty_if_resource_is_not_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    ResourceWithOptionalRelated proxy = createClientProxy(ResourceWithOptionalRelated.class);

    Maybe<ResourceWithSingleState> maybeEmbedded = proxy.getOptionalItem();

    assertThat(maybeEmbedded.isEmpty().blockingGet()).isTrue();
  }


  @HalApiInterface
  interface ResourceWithMultipleRelated {

    @RelatedResource(relation = ITEM)
    Observable<ResourceWithSingleState> getItems();
  }

  @Test
  public void observable_linked_resource_should_emitted_single_item() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    ResourceWithMultipleRelated proxy = createClientProxy(ResourceWithMultipleRelated.class);

    TestResourceState linkedState = proxy.getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .firstOrError().blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void observable_linked_resource_should_be_empty_if_no_links_are_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createLinked(ALTERNATE).setText("item text");

    ResourceWithMultipleRelated proxy = createClientProxy(ResourceWithMultipleRelated.class);

    Observable<ResourceWithSingleState> rxLinkedResources = proxy.getItems();

    assertThat(rxLinkedResources.isEmpty().blockingGet()).isTrue();
  }

  @Test
  public void observable_linked_resource_should_emitted_multiple_items() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createLinked(ITEM).setNumber(i));

    ResourceWithMultipleRelated proxy = createClientProxy(ResourceWithMultipleRelated.class);

    List<TestResourceState> linkedStates = proxy.getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .toList().blockingGet();

    assertThat(linkedStates).hasSize(10);
    for (int i = 0; i < numItems; i++) {
      assertThat(linkedStates.get(i).number).isEqualTo(i);
    }
  }

  @Test
  public void observable_embedded_resource_should_emitted_single_item() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    ResourceWithMultipleRelated proxy = createClientProxy(ResourceWithMultipleRelated.class);

    TestResourceState embeddedState = proxy.getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .firstOrError().blockingGet();

    assertThat(embeddedState).isNotNull();
    assertThat(embeddedState.text).isEqualTo("item text");
  }

  @Test
  public void observable_embedded_resource_should_be_empty_if_no_resources_are_present() throws Exception {

    // create an embeded resource with a different relation then defined in the interface
    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    ResourceWithMultipleRelated proxy = createClientProxy(ResourceWithMultipleRelated.class);

    Observable<ResourceWithSingleState> rxEmbeddedResources = proxy.getItems();

    assertThat(rxEmbeddedResources.isEmpty().blockingGet()).isTrue();
  }

  @Test
  public void observable_embedded_resource_should_emitted_multiple_items() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createEmbedded(ITEM).setNumber(i));

    ResourceWithMultipleRelated proxy = createClientProxy(ResourceWithMultipleRelated.class);

    List<TestResourceState> embeddedStates = proxy.getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .toList().blockingGet();

    assertThat(embeddedStates).hasSize(10);
    for (int i = 0; i < numItems; i++) {
      assertThat(embeddedStates.get(i).number).isEqualTo(i);
    }
  }

  @Test
  public void duplicates_should_be_filtered_if_they_are_linked_and_embedded() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> {
      TestResource item = entryPoint.createLinked(ITEM).setNumber(i);
      entryPoint.asHalResource().addEmbedded(ITEM, item.asHalResource());
    });

    ResourceWithMultipleRelated proxy = createClientProxy(ResourceWithMultipleRelated.class);

    List<TestResourceState> embeddedStates = proxy.getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .toList().blockingGet();

    assertThat(embeddedStates).hasSize(10);
    for (int i = 0; i < numItems; i++) {
      assertThat(embeddedStates.get(i).number).isEqualTo(i);
    }
  }
}
