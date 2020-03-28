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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.ConversionFunctions;
import io.wcm.caravan.hal.microservices.testing.resources.TestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;
import io.wcm.caravan.hal.resource.HalResource;

public class ClientTestSupport {

  static final String ENTRY_POINT_URI = "/";

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

  protected final JsonResourceLoader jsonLoader;
  protected final TestResourceTree testResourceTree;

  protected ClientTestSupport(JsonResourceLoader jsonLoader) {
    this.jsonLoader = jsonLoader;
    this.testResourceTree = null;
  }

  protected ClientTestSupport(TestResourceTree testResourceTree) {
    this.jsonLoader = testResourceTree;
    this.testResourceTree = testResourceTree;
  }

  <T> T createProxy(Class<T> halApiInterface) {
    HalApiClient client = HalApiClient.create(jsonLoader, metrics);
    T clientProxy = client.getEntryPoint(ENTRY_POINT_URI, halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  RequestMetricsCollector getMetrics() {
    return metrics;
  }

  static ResourceTreeClientTestSupport withResourceTree() {
    return new ResourceTreeClientTestSupport();
  }

  static MockClientTestSupport withMocking() {
    return new MockClientTestSupport();
  }

  static class ResourceTreeClientTestSupport extends ClientTestSupport {

    public ResourceTreeClientTestSupport() {
      super(new TestResourceTree());
    }

    TestResource getEntryPoint() {
      return testResourceTree.getEntryPoint();
    }
  }

  static class MockClientTestSupport extends ClientTestSupport {


    MockClientTestSupport() {
      super(Mockito.mock(JsonResourceLoader.class));
    }

    void mockResponseWithSupplier(String uri, Supplier<Single<HalResponse>> supplier) {

      when(jsonLoader.loadJsonResource(uri))
          .thenAnswer(new Answer<Single<HalResponse>>() {

            @Override
            public Single<HalResponse> answer(InvocationOnMock invocation) throws Throwable {
              return supplier.get();
            }
          });
    }

    void mockResponseWithSingle(String uri, Single<HalResponse> value) {

      when(jsonLoader.loadJsonResource(uri))
          .thenReturn(value);
    }

    void mockFailedResponse(String uri, Integer statusCode) {

      HalApiClientException hace = new HalApiClientException("Simulated failed response", statusCode, uri, null);

      mockResponseWithSingle(uri, Single.error(hace));
    }

    SingleSubject<HalResource> mockHalResponseWithSubject(String uri) {

      SingleSubject<HalResource> testSubject = SingleSubject.create();

      mockResponseWithSingle(uri, testSubject.map(ConversionFunctions::toJsonResponse));

      return testSubject;
    }

    void mockHalResponse(String uri, HalResource hal) {

      HalResponse response = ConversionFunctions.toJsonResponse(hal);

      mockResponseWithSingle(uri, Single.just(response));
    }

    void mockHalResponseWithState(String uri, Object state) {

      HalResource hal = new HalResource(state, uri);

      mockHalResponse(uri, hal);
    }
  }
}
