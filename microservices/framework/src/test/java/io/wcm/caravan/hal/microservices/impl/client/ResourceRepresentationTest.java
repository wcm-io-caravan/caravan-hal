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
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.ResourceRepresentation;
import io.wcm.caravan.hal.api.relations.StandardRelations;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.resources.TestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;
import io.wcm.caravan.hal.resource.HalResource;

public class ResourceRepresentationTest {

  private RequestMetricsCollector metrics;
  private JsonResourceLoader jsonLoader;
  private TestResource entryPoint;

  @Before
  public void setUp() {
    metrics = RequestMetricsCollector.create();

    TestResourceTree testResourceTree = new TestResourceTree();
    jsonLoader = testResourceTree;
    entryPoint = testResourceTree.getEntryPoint();

    entryPoint.setText("test");
    entryPoint.setFlag(true);
    entryPoint.createLinked(ITEM);
    entryPoint.createEmbedded(StandardRelations.COLLECTION).createEmbedded(ITEM);

  }

  private <T> T createClientProxy(Class<T> halApiInterface) {
    HalApiClient client = HalApiClient.create(jsonLoader, metrics);
    T clientProxy = client.getEntryPoint(entryPoint.getUrl(), halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  @HalApiInterface
  interface ResourceWithRepresentations {

    @ResourceRepresentation
    Single<HalResource> asHalResource();

    @ResourceRepresentation
    Single<ObjectNode> asObjectNode();

    @ResourceRepresentation
    Single<JsonNode> asJsonNode();

    @ResourceRepresentation
    Single<String> asString();
  }

  @Test
  public void representation_should_be_available_as_hal_resource() {


    HalResource hal = createClientProxy(ResourceWithRepresentations.class)
        .asHalResource()
        .blockingGet();

    assertThat(hal.getModel()).isEqualTo(entryPoint.getJson());
  }

  @Test
  public void representation_should_be_available_as_object_node() {

    ObjectNode json = createClientProxy(ResourceWithRepresentations.class)
        .asObjectNode()
        .blockingGet();

    assertThat(json).isEqualTo(entryPoint.getJson());
  }

  @Test
  public void representation_should_be_available_as_json_node() {

    JsonNode json = createClientProxy(ResourceWithRepresentations.class)
        .asJsonNode()
        .blockingGet();

    assertThat(json).isEqualTo(entryPoint.getJson());
  }

  @Test
  public void representation_should_be_available_as_string() {

    String string = createClientProxy(ResourceWithRepresentations.class)
        .asString()
        .blockingGet();

    assertThat(string).isEqualTo(entryPoint.getJson().toString());
  }

  @HalApiInterface
  interface ResourceWithUnsupportedRepresentations {

    @ResourceRepresentation
    Single<Document> asXmlDocument();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_unsupported_operation_if_emission_type_is_not_supported() {

    createClientProxy(ResourceWithUnsupportedRepresentations.class)
        .asXmlDocument()
        .blockingGet();
  }
}
