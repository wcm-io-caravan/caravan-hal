/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
import static io.wcm.caravan.hal.microservices.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceRepresentation;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.hal.microservices.testing.LinkableTestResource;
import io.wcm.caravan.hal.microservices.testing.TestState;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class ProxyCachingTest {

  private final static String ITEM_1_URL = "/item/1";
  private final static String ITEM_2_URL = "/item/2";

  private final MockClientTestSupport client = ClientTestSupport.withMocking();
  private final HalResource entryPointHal = new HalResource(ENTRY_POINT_URI);

  @BeforeEach
  void setUp() {
    client.mockHalResponse(ENTRY_POINT_URI, entryPointHal);

    addLinkAndMockResource(ITEM_1_URL);
    addLinkAndMockResource(ITEM_2_URL);
  }

  private void addLinkAndMockResource(String itemUrl) {
    TestState state = new TestState(itemUrl);
    HalResource linkedItem = new HalResource(state, itemUrl);
    entryPointHal.addLinks(ITEM, new Link(itemUrl));
    client.mockHalResponse(itemUrl, linkedItem);
  }

  @HalApiInterface
  interface EntryPoint {

    @ResourceState
    Maybe<TestState> getState();

    @RelatedResource(relation = ITEM)
    Observable<LinkableTestResource> getLinked();

    @ResourceRepresentation
    Single<HalResource> asHalResource();
  }

  @HalApiInterface
  interface LinkedResource {

    @ResourceState
    Maybe<TestState> getState();
  }

  @Test
  public void multiple_calls_to_entrypoint_should_return_the_same_proxy_instance_for_the_same_interface() {

    HalApiClient halApiClient = client.getHalApiClient();

    EntryPoint proxy1 = halApiClient.getEntryPoint(ENTRY_POINT_URI, EntryPoint.class);
    EntryPoint proxy2 = halApiClient.getEntryPoint(ENTRY_POINT_URI, EntryPoint.class);

    assertThat(proxy1).isSameAs(proxy2);
  }

  @HalApiInterface
  interface AltEntryPointInterface {

    @ResourceState
    Maybe<TestState> getState();
  }

  @Test
  public void multiple_calls_to_entrypoint_should_not_return_the_same_proxy_instance_for_a_different_interface() {

    HalApiClient halApiClient = client.getHalApiClient();

    EntryPoint proxy1 = halApiClient.getEntryPoint(ENTRY_POINT_URI, EntryPoint.class);
    AltEntryPointInterface proxy2 = halApiClient.getEntryPoint(ENTRY_POINT_URI, AltEntryPointInterface.class);

    assertThat(proxy1).isNotSameAs(proxy2);
  }

  @Test
  public void multiple_calls_to_state_method_should_return_the_same_observable() {

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    Maybe<TestState> state1 = entryPoint.getState();
    Maybe<TestState> state2 = entryPoint.getState();

    assertThat(state1).isSameAs(state2);
  }

  @Test
  public void multiple_calls_to_related_resource_method_should_return_the_same_observable() {

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    Observable<LinkableTestResource> linked1 = entryPoint.getLinked();
    Observable<LinkableTestResource> linked2 = entryPoint.getLinked();

    assertThat(linked1).isSameAs(linked2);
  }

  @Test
  public void observables_from_multiple_calls_emit_the_same_caching_instances() {

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    Observable<LinkableTestResource> linked1 = entryPoint.getLinked();
    Observable<LinkableTestResource> linked2 = entryPoint.getLinked();

    List<TestState> list1 = linked1.concatMapMaybe(LinkableTestResource::getState).toList().blockingGet();
    List<TestState> list2 = linked2.concatMapMaybe(LinkableTestResource::getState).toList().blockingGet();

    assertThat(list1).containsExactlyElementsOf(list2);

    // verify that json for each item was only loaded once
    verify(client.getMockJsonLoader()).loadJsonResource(ITEM_1_URL);
    verify(client.getMockJsonLoader()).loadJsonResource(ITEM_2_URL);
  }
}
