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

import static io.wcm.caravan.hal.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.LinkName;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.hal.microservices.testing.resources.TestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceState;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class LinkNameTest {

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
  interface LinkedResource {

    @ResourceState
    Single<TestResourceState> getState();

    @ResourceLink
    Link createLink();
  }


  @HalApiInterface
  interface ResourceWithNamedLinked {

    @RelatedResource(relation = ITEM)
    Maybe<LinkedResource> getLinkedByName(@LinkName String linkName);
  }

  @Test
  public void should_find_existing_named_link() {

    Observable.range(0, 10).forEach(i -> {
      String linkName = Integer.toString(i);
      entryPoint.createLinked(ITEM, linkName).setNumber(i);
    });

    String linkNameToFind = "5";

    TestResourceState state = createClientProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(linkNameToFind)
        .flatMapSingle(LinkedResource::getState)
        .blockingGet();

    assertThat(state.number).isEqualTo(5);
  }

  @Test
  public void should_return_empty_maybe_for_missing_named_link() {

    Observable.range(0, 10).forEach(i -> {
      String linkName = Integer.toString(i);
      entryPoint.createLinked(ITEM, linkName).setNumber(i);
    });

    String linkNameToFind = "missing";

    Maybe<LinkedResource> maybeLinked = createClientProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(linkNameToFind);

    assertThat(maybeLinked.isEmpty().blockingGet()).isEqualTo(true);
  }

  @Test
  public void should_find_existing_embedded_if_named_link_with_same_href_is_also_present_in_the_resource() {

    Observable.range(0, 10).forEach(i -> {
      String linkName = Integer.toString(i);
      TestResource itemResource = entryPoint.createLinked(ITEM, linkName).setNumber(i);
      entryPoint.asHalResource().addEmbedded(ITEM, itemResource.asHalResource());
    });

    String linkNameToFind = "5";

    TestResourceState state = createClientProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(linkNameToFind)
        .flatMapSingle(LinkedResource::getState)
        .blockingGet();

    assertThat(state.number).isEqualTo(5);
  }

  @Test
  public void should_find_existing_named_linked_even_if_unnamed_embedded_items_are_present() {

    Observable.range(0, 10).forEach(i -> {
      String linkName = Integer.toString(i);
      entryPoint.createLinked(ITEM, linkName).setNumber(i);
      entryPoint.asHalResource().addEmbedded(ITEM, new HalResource());
    });

    String linkNameToFind = "5";

    TestResourceState state = createClientProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(linkNameToFind)
        .flatMapSingle(LinkedResource::getState)
        .blockingGet();

    assertThat(state.number).isEqualTo(5);
  }


  @Test(expected = IllegalArgumentException.class)
  public void should_fail_if_null_is_given_as_link_name() {

    createClientProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(null);
  }


  @HalApiInterface
  interface ResourceWithMultipleAnnotations {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getItem(@LinkName String parameter, @LinkName String other);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_unsupported_operation_if_multiple_link_name_parameters_are_present() {

    createClientProxy(ResourceWithMultipleAnnotations.class)
        .getItem("foo", "bar");
  }
}
