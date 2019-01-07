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
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.api.server.testing.TestState;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.resources.TestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;

public class MaxAgeTest {

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
    HalApiClient client = HalApiClient.create(jsonLoader, binaryLoader, metrics);
    T clientProxy = client.getEntryPoint(entryPoint.getUrl(), halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }


  @HalApiInterface
  interface EntryPoint {

    @ResourceState
    Maybe<TestState> getState();

    @RelatedResource(relation = ITEM)
    Observable<LinkedResource> getLinked();

  }

  @HalApiInterface
  interface LinkedResource {

    @ResourceState
    Maybe<TestState> getState();
  }

  private void loadEntryPoint() {

    createClientProxy(EntryPoint.class)
        .getState()
        .blockingGet();
  }

  @Test
  public void max_age_should_be_null_if_nothing_was_specified() {

    loadEntryPoint();

    assertThat(metrics.getOutputMaxAge()).isNull();
  }


  @Test
  public void explicit_max_age_should_be_used_if_no_headers_found_in_response() {

    metrics.limitOutputMaxAge(45);

    loadEntryPoint();

    assertThat(metrics.getOutputMaxAge()).isEqualTo(45);
  }


  @Test
  public void max_age_from_entrypoint_response_should_be_used_if_no_explicit_value_defined() {

    entryPoint.withMaxAge(55);

    loadEntryPoint();

    assertThat(metrics.getOutputMaxAge()).isEqualTo(55);
  }

  @Test
  public void max_age_from_entrypoint_response_should_be_used_if_smaller_than_explicit_value() {

    metrics.limitOutputMaxAge(125);
    entryPoint.withMaxAge(55);

    loadEntryPoint();

    assertThat(metrics.getOutputMaxAge()).isEqualTo(55);
  }

  @Test
  public void explicit_max_age_should_be_used_if_smaller_than_header_from_entry_point() {

    metrics.limitOutputMaxAge(45);
    entryPoint.withMaxAge(180);

    loadEntryPoint();

    assertThat(metrics.getOutputMaxAge()).isEqualTo(45);
  }

  @Test
  public void smallest_max_age_from_all_loaded_resources_should_be_used() {

    entryPoint.withMaxAge(55);
    entryPoint.createLinked(ITEM).withMaxAge(15);
    entryPoint.createLinked(ITEM).withMaxAge(85);

    createClientProxy(EntryPoint.class)
        .getLinked().flatMapMaybe(LinkedResource::getState)
        .toList().blockingGet();

    assertThat(metrics.getOutputMaxAge()).isEqualTo(15);
  }

  @Test
  public void resources_without_max_age_header_should_be_ignored() {

    entryPoint.withMaxAge(55);
    entryPoint.createLinked(ITEM);
    entryPoint.createLinked(ITEM).withMaxAge(85);

    createClientProxy(EntryPoint.class)
        .getLinked().flatMapMaybe(LinkedResource::getState)
        .toList().blockingGet();

    assertThat(metrics.getOutputMaxAge()).isEqualTo(55);
  }
}
