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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.damnhandy.uri.template.UriTemplate;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.TemplateVariable;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.hal.microservices.testing.ConversionFunctions;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceState;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class TemplateVariableTest {

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

    when(jsonLoader.loadJsonResource(eq(url)))
        .thenReturn(Single.just(ConversionFunctions.toJsonResponse(resource)));
  }

  private void mockHalResponseWithNumber(String url, int number) {
    TestResourceState state = new TestResourceState();
    state.number = number;

    HalResource resource = new HalResource(state, url);
    mockHalResponse(url, resource);
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {

    HalApiClient client = HalApiClient.create(jsonLoader, binaryLoader, metrics);
    T clientProxy = client.getEntryPoint(ENTRYPOINT_URL, halApiInterface);

    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  @HalApiInterface
  interface ResourceWithSimpleLinkTemplate {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getLinked(@TemplateVariable("number") Integer number);
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
    Single<ResourceWithSingleState> getLinked(
        @TemplateVariable("number") Integer number,
        @TemplateVariable("optionalFlag") Boolean optionalFlag);
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

  @HalApiInterface
  interface ResourceWithTemplateAndResolvedLinks {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getLinked(@TemplateVariable("number") Integer number);

    @RelatedResource(relation = ITEM)
    Observable<ResourceWithSingleState> getAllLinked();
  }

  @Test
  public void ignore_link_template_if_method_without_template_variable_is_called_and_there_are_resolved_links() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    int numResolvedLinks = 5;
    Observable.range(0, numResolvedLinks).forEach(i -> {
      String url = "/item/" + i;
      entryPoint.addLinks(ITEM, new Link(url));

      mockHalResponseWithNumber(url, i);
    });

    List<TestResourceState> states = createClientProxy(ResourceWithTemplateAndResolvedLinks.class)
        .getAllLinked()
        .flatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

    assertThat(states).hasSize(numResolvedLinks);
  }

  @Test
  public void expand_link_template_if_method_without_template_variable_is_called_but_there_are_no_resolved_links() {

    int numTemplates = 5;
    Observable.range(0, numTemplates).forEach(i -> {
      String template = "/item/" + i + "{?optionalFlag}";
      entryPoint.addLinks(ITEM, new Link(template));

      String uri = UriTemplate.fromTemplate(template).expand();
      mockHalResponseWithNumber(uri, i);
    });

    List<TestResourceState> states = createClientProxy(ResourceWithTemplateAndResolvedLinks.class)
        .getAllLinked()
        .flatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

    assertThat(states).hasSize(numTemplates);
  }

  @Test
  public void resolved_links_should_be_ignored_if_method_with_template_variable_is_called() {

    int numResolvedLinks = 5;
    Observable.range(0, numResolvedLinks).forEach(i -> {
      String url = "/item/" + i;
      entryPoint.addLinks(ITEM, new Link(url));

      mockHalResponseWithNumber(url, i);
    });

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    TestResourceState state = createClientProxy(ResourceWithTemplateAndResolvedLinks.class)
        .getLinked(3)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(3);
  }

  @Test
  public void resolved_links_should_be_followed_if_method_with_template_variable_is_called_but_there_is_no_template() {

    String url = "/item/3";
    entryPoint.addLinks(ITEM, new Link(url));

    mockHalResponseWithNumber(url, 3);

    TestResourceState state = createClientProxy(ResourceWithTemplateAndResolvedLinks.class)
        .getLinked(3)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(3);
  }


  @HalApiInterface
  interface ResourceWithMissingAnnotations {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getItem(String parameter);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_unsupported_operation_if_annotation_for_parameter_is_missing() {

    createClientProxy(ResourceWithMissingAnnotations.class)
        .getItem("foo");
  }
}
