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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceState;
import io.wcm.caravan.hal.resource.HalResource;


public class ResourceStateTest {

  private static final String RESOURCE_URL = "/";

  private RequestMetricsCollector metrics;
  private BinaryResourceLoader binaryLoader;
  private JsonResourceLoader jsonLoader;

  @Before
  public void setUp() {
    metrics = RequestMetricsCollector.create();
    binaryLoader = Mockito.mock(BinaryResourceLoader.class);
    jsonLoader = Mockito.mock(JsonResourceLoader.class);
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {
    HalApiClientImpl client = new HalApiClientImpl(jsonLoader, binaryLoader, metrics);
    T clientProxy = client.getEntryPoint(RESOURCE_URL, halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  private void mockHalResponseWithSingle(Object state) {

    when(jsonLoader.loadJsonResource(eq(RESOURCE_URL), eq(metrics)))
        .thenReturn(Single.just(new HalResource(state, RESOURCE_URL).getModel()));
  }

  @HalApiInterface
  interface ResourceWithSingleState {

    @ResourceState
    Single<TestResourceState> getProperties();
  }

  @Test
  public void single_resource_state_should_be_emitted() throws Exception {

    mockHalResponseWithSingle(new TestResourceState().withText("test"));

    TestResourceState properties = createClientProxy(ResourceWithSingleState.class)
        .getProperties()
        .blockingGet();

    assertThat(properties).isNotNull();
    assertThat(properties.text).isEqualTo("test");
  }


  @HalApiInterface
  interface ResourceWithOptionalState {

    @ResourceState
    Maybe<TestResourceState> getProperties();
  }

  @Test
  public void maybe_resource_state_should_be_emitted() throws Exception {

    mockHalResponseWithSingle(new TestResourceState().withText("test"));

    TestResourceState properties = createClientProxy(ResourceWithOptionalState.class)
        .getProperties()
        .blockingGet();

    assertThat(properties).isNotNull();
    assertThat(properties.text).isEqualTo("test");
  }

  @Test
  public void maybe_resource_state_should_be_empty_if_no_properties_are_set() throws Exception {

    mockHalResponseWithSingle(JsonNodeFactory.instance.objectNode());

    TestResourceState properties = createClientProxy(ResourceWithOptionalState.class)
        .getProperties()
        .blockingGet();

    assertThat(properties).isNull();
  }


  @HalApiInterface
  interface ResourceWithIllegalAnnotations {

    @ResourceState
    TestResourceState notReactive();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_unsupported_operation_if_return_type_is_not_reactive() {

    createClientProxy(ResourceWithIllegalAnnotations.class)
        .notReactive();
  }

}
