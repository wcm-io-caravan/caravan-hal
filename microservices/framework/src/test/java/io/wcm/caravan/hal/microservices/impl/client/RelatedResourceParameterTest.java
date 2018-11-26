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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceState;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class RelatedResourceParameterTest {

  private static final String ENTRYPOINT_URL = "/";
  private RequestMetricsCollector metrics;
  private BinaryResourceLoader binaryLoader;
  private JsonResourceLoader jsonLoader;
  private HalResource entryPoint;

  @Before
  public void setUp() {
    metrics = RequestMetricsCollector.create();
    binaryLoader = Mockito.mock(BinaryResourceLoader.class);
    jsonLoader = Mockito.mock(JsonResourceLoader.class);
    entryPoint = new HalResource();

    mockHalResponse(ENTRYPOINT_URL, entryPoint);
  }

  private void mockHalResponse(String url, HalResource resource) {

    when(jsonLoader.loadJsonResource(eq(url), eq(metrics)))
        .thenReturn(Single.just(resource.getModel()));
  }

  private void mockHalResponseWithNumber(String url, int number) {
    TestResourceState state = new TestResourceState();
    state.number = number;

    HalResource resource = new HalResource(state, url);
    mockHalResponse(url, resource);
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {

    HalApiClientImpl client = new HalApiClientImpl(jsonLoader, binaryLoader, metrics);
    T clientProxy = client.getEntryPoint(ENTRYPOINT_URL, halApiInterface);

    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  @HalApiInterface
  interface ResourceWithSimpleLinkTemplate {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getLinked(Integer number);
  }

  @Test
  public void link_template_should_be_expanded_if_only_parameter_is_given() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    mockHalResponseWithNumber("/item/1", 1);

    TestResourceState state = createClientProxy(ResourceWithSimpleLinkTemplate.class)
        .getLinked(1)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void link_template_should_not_be_expanded_if_only_parameter_is_missing() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    mockHalResponseWithNumber("/item/", 0);

    createClientProxy(ResourceWithSimpleLinkTemplate.class)
        .getLinked(null)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();
  }

  @HalApiInterface
  interface ResourceWithComplexLinkTemplate {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getLinked(Integer number, Boolean optionalFlag);
  }

  @Test
  public void link_template_should_be_expanded_if_one_of_multiple_parameters_is_missing() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}{?optionalFlag}"));

    mockHalResponseWithNumber("/item/1", 1);

    TestResourceState state = createClientProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(1, null)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

  @Test
  public void link_template_should_be_expanded_if_all_of_multiple_parameters_are_present() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}{?optionalFlag}"));

    mockHalResponseWithNumber("/item/1?optionalFlag=true", 1);

    TestResourceState state = createClientProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(1, true)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

}
