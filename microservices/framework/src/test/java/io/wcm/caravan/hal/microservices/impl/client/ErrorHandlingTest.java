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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceRepresentation;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.LinkableTestResource;
import io.wcm.caravan.hal.microservices.testing.TestState;
import io.wcm.caravan.hal.resource.HalResource;

public class ErrorHandlingTest {

  private static final String ENTRY_POINT_URI = "/";

  private RequestMetricsCollector metrics;
  private JsonResourceLoader jsonLoader;

  @Before
  public void setUp() {
    metrics = RequestMetricsCollector.create();
    jsonLoader = Mockito.mock(JsonResourceLoader.class);
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {
    HalApiClient client = HalApiClient.create(jsonLoader, metrics);
    T clientProxy = client.getEntryPoint(ENTRY_POINT_URI, halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  private void mockFailedResponse(Integer statusCode, String uri) {

    when(jsonLoader.loadJsonResource(uri))
        .thenReturn(Single.error(new HalApiClientException("Simulated failed response", statusCode, uri, null)));
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
  public void status_code_from_response_should_be_available_in_exception_when_calling_ResourceState_method() {

    mockFailedResponse(403, ENTRY_POINT_URI);

    try {
      createClientProxy(EntryPoint.class)
          .getState()
          .blockingGet();

      fail("Expected " + HalApiClientException.class.getSimpleName() + " was not thrown");
    }
    catch (HalApiClientException ex) {
      assertThat(ex.getStatusCode()).isEqualTo(403);
    }
  }

  @Test
  public void status_code_from_response_should_be_available_in_exception_when_calling_RelatedResource_method() {

    mockFailedResponse(501, ENTRY_POINT_URI);

    try {
      createClientProxy(EntryPoint.class)
          .getLinked()
          .flatMapMaybe(LinkableTestResource::getState)
          .toList().blockingGet();

      fail("Expected " + HalApiClientException.class.getSimpleName() + " was not thrown");
    }
    catch (HalApiClientException ex) {
      assertThat(ex.getStatusCode()).isEqualTo(501);
    }
  }

  @Test
  public void status_code_from_response_should_be_available_in_exception_when_calling_ResourceRepresentation_method() {

    mockFailedResponse(502, ENTRY_POINT_URI);

    try {
      createClientProxy(EntryPoint.class)
          .asHalResource()
          .blockingGet();

      fail("Expected " + HalApiClientException.class.getSimpleName() + " was not thrown");
    }
    catch (HalApiClientException ex) {
      assertThat(ex.getStatusCode()).isEqualTo(502);
    }
  }

  @Test
  public void status_code_from_response_can_be_null_if_request_failed_with_network_issues() {

    mockFailedResponse(null, ENTRY_POINT_URI);

    try {
      createClientProxy(EntryPoint.class)
          .getState()
          .blockingGet();

      fail("Expected " + HalApiClientException.class.getSimpleName() + " was not thrown");
    }
    catch (HalApiClientException ex) {
      assertThat(ex.getStatusCode()).isNull();
    }
  }

  interface EntryPointWithoutAnnotation {

    @ResourceState
    Maybe<TestState> getState();

    @RelatedResource(relation = ITEM)
    Observable<LinkedResource> getLinked();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_unsupported_operation_if_HalApiAnnotation_is_missing() {

    createClientProxy(EntryPointWithoutAnnotation.class).getState().blockingGet();
  }
}