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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.api.annotations.TemplateVariable;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.resources.TestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class ResourceLinkTest {

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
  interface LinkableResource {

    @ResourceLink
    Link createLink();
  }

  @Test
  public void link_should_be_extracted_from_entry_point() {

    Link link = createClientProxy(LinkableResource.class)
        .createLink();

    assertThat(link.getHref()).isEqualTo(entryPoint.getUrl());
  }


  @HalApiInterface
  interface ResourceWithSingleLinked {

    @RelatedResource(relation = ITEM)
    Single<LinkableResource> getLinked();
  }

  @Test
  public void link_should_be_extracted_from_single_linked_resource() {

    TestResource itemResource = entryPoint.createLinked(ITEM);

    Link link = createClientProxy(ResourceWithSingleLinked.class)
        .getLinked()
        .map(LinkableResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(itemResource.getUrl());
  }

  @Test
  public void original_link_name_should_be_available() {

    String linkName = "linkName";

    entryPoint.createLinked(ITEM, linkName);

    Link link = createClientProxy(ResourceWithSingleLinked.class)
        .getLinked()
        .map(LinkableResource::createLink)
        .blockingGet();

    assertThat(link.getName()).isEqualTo(linkName);
  }


  @HalApiInterface
  interface ResourceWithMultipleLinked {

    @RelatedResource(relation = ITEM)
    Observable<LinkableResource> getLinked();
  }

  @Test
  public void filtering_linked_resources_by_name_should_be_possible() {

    Observable.range(0, 10).forEach(i -> entryPoint.createLinked(ITEM, Integer.toString(i)));

    String linkNameToFind = "5";

    Link filteredLink = createClientProxy(ResourceWithMultipleLinked.class)
        .getLinked()
        .filter(resource -> StringUtils.equals(resource.createLink().getName(), "5"))
        .map(LinkableResource::createLink)
        .singleElement()
        .blockingGet();

    assertThat(filteredLink.getName()).isEqualTo(linkNameToFind);
  }

  @HalApiInterface
  interface ResourceWithSingleEmbedded {

    @RelatedResource(relation = ITEM)
    Single<LinkableResource> getEmbedded();
  }

  @Test
  public void self_link_should_be_extracted_from_embedded_resource_if_its_not_explicitly_linked() {

    TestResource itemResource = entryPoint.createEmbedded(ITEM);
    itemResource.asHalResource().setLink(new Link("/embedded/self-link"));

    Link link = createClientProxy(ResourceWithSingleEmbedded.class)
        .getEmbedded()
        .map(LinkableResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(itemResource.getUrl());
  }

  @Test
  public void named_link_should_be_extracted_for_embedded_resource_if_it_is_explicitly_linked() {

    String linkName = "linkName";
    TestResource itemResource = entryPoint.createLinked(ITEM, linkName);
    entryPoint.asHalResource().addEmbedded(ITEM, new HalResource(itemResource.getUrl()));

    Link link = createClientProxy(ResourceWithSingleEmbedded.class)
        .getEmbedded()
        .map(LinkableResource::createLink)
        .blockingGet();

    assertThat(link.getName()).isEqualTo(linkName);
  }

  @Test
  public void link_with_empty_href_should_be_extracted_from_embedded_resource_without_self_link() {

    entryPoint.createEmbedded(ITEM);

    Link link = createClientProxy(ResourceWithSingleEmbedded.class)
        .getEmbedded()
        .map(LinkableResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEmpty();
  }


  @HalApiInterface
  interface ResourceWithLinkTemplate {

    @RelatedResource(relation = ITEM)
    Single<LinkableResource> getLinked(
        @TemplateVariable("intParam") Integer intParam,
        @TemplateVariable("stringParam") String stringParam,
        @TemplateVariable("listParam") List<String> listParam);
  }

  @Test
  public void link_template_should_be_fully_expanded_if_at_least_one_parameter_is_null() {

    String uriTemplate = "/test{?intParam,stringParam,listParam*}";
    entryPoint.asHalResource().addLinks(ITEM, new Link(uriTemplate));

    Link link = createClientProxy(ResourceWithLinkTemplate.class)
        .getLinked(5, null, null)
        .map(LinkableResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo("/test?intParam=5");
  }

  @Test
  public void link_template_should_not_be_expanded_if_all_parameters_are_null() {

    String uriTemplate = "/test{?intParam,stringParam,listParam*}";
    entryPoint.asHalResource().addLinks(ITEM, new Link(uriTemplate));

    Link link = createClientProxy(ResourceWithLinkTemplate.class)
        .getLinked(null, null, null)
        .map(LinkableResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(uriTemplate);
  }
}
