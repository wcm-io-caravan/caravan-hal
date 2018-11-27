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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.damnhandy.uri.template.UriTemplate;

import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.api.annotations.TemplateVariables;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceState;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class TemplateVariablesTest {

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

  private void mockHalResponseWithNumberAndText(String url, Integer number, String text) {
    TestResourceState state = new TestResourceState();
    state.number = number;
    state.text = text;

    HalResource resource = new HalResource(state, url);
    mockHalResponse(url, resource);
  }

  private void mockHalResponseForTemplateExpandedWithDto(String template, SimpleDto dto) {

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", dto.id);
    map.put("text", dto.text);

    String uri = UriTemplate.expand(template, map);
    mockHalResponseWithNumberAndText(uri, dto.id, dto.text);
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {

    HalApiClientImpl client = new HalApiClientImpl(jsonLoader, binaryLoader, metrics);
    T clientProxy = client.getEntryPoint(ENTRYPOINT_URL, halApiInterface);

    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  @HalApiInterface
  interface LinkedResourceWithSingleState {

    @ResourceState
    Single<TestResourceState> getProperties();

    @ResourceLink
    Link createLink();
  }

  @TemplateVariables
  public static class SimpleDto {

    public Integer id;
    public String text;
  }

  @HalApiInterface
  interface ResourceWithTemplateVariables {

    @RelatedResource(relation = ITEM)
    Single<LinkedResourceWithSingleState> getItem(SimpleDto dto);
  }

  @Test
  public void should_expand_template_with_simple_dto() throws Exception {

    String template = new String("/item/{id}{?text*}");
    entryPoint.addLinks(ITEM, new Link(template));

    SimpleDto dto = new SimpleDto();
    dto.id = 123;
    dto.text = "text";

    mockHalResponseForTemplateExpandedWithDto(template, dto);

    TestResourceState linkedState = createClientProxy(ResourceWithTemplateVariables.class)
        .getItem(dto)
        .flatMap(LinkedResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
  }

  @Test
  public void should_expand_template_with_null_field_in_simple_dto() throws Exception {

    String template = new String("/item/{id}{?text}");
    entryPoint.addLinks(ITEM, new Link(template));

    SimpleDto dto = new SimpleDto();
    dto.id = 123;
    dto.text = null;

    mockHalResponseForTemplateExpandedWithDto(template, dto);

    TestResourceState linkedState = createClientProxy(ResourceWithTemplateVariables.class)
        .getItem(dto)
        .flatMap(LinkedResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
  }

  @Test
  public void should_expand_template_with_only_null_fields_in_simple_dto() throws Exception {

    String template = new String("/item/{id}{?text}");
    entryPoint.addLinks(ITEM, new Link(template));

    SimpleDto dto = new SimpleDto();
    dto.id = null;
    dto.text = null;

    mockHalResponseForTemplateExpandedWithDto(template, dto);

    TestResourceState linkedState = createClientProxy(ResourceWithTemplateVariables.class)
        .getItem(dto)
        .flatMap(LinkedResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
  }

  @Test
  public void should_not_expand_template_if_null_dto_is_used() throws Exception {

    String template = new String("/item/{id}{?text}");
    entryPoint.addLinks(ITEM, new Link(template));

    Link link = createClientProxy(ResourceWithTemplateVariables.class)
        .getItem(null)
        .map(LinkedResourceWithSingleState::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(template);
  }
}
