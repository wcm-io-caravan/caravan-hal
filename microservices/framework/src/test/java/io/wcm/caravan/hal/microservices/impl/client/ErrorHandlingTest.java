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

import static io.wcm.caravan.hal.api.annotations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

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
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.resources.TestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;

public class ErrorHandlingTest {

  private RequestMetricsCollector metrics;
  private BinaryResourceLoader binaryLoader;
  private TestResourceTree jsonLoader;
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

  private void loadEntryPoint() {

    createClientProxy(EntryPoint.class)
        .getState()
        .blockingGet();
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

  @Test
  public void status_code_from_failed_responses_should_be_present_in_exception() {

    entryPoint.withStatus(404);

    try {
      loadEntryPoint();
      fail("Expected " + HalApiClientException.class.getSimpleName() + " was not thrown");
    }
    catch (HalApiClientException ex) {
      assertThat(ex.getStatusCode()).isEqualTo(404);
    }
  }
}
