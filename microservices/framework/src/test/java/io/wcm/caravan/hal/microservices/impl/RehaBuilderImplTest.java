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
package io.wcm.caravan.hal.microservices.impl;

import static io.wcm.caravan.hal.microservices.impl.metadata.ResponseMetadataRelations.CARAVAN_METADATA_RELATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.caravan.hal.microservices.api.Reha;
import io.wcm.caravan.hal.microservices.api.RehaBuilder;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.hal.microservices.testing.LinkableTestResource;
import io.wcm.caravan.hal.microservices.testing.TestState;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

@ExtendWith(MockitoExtension.class)
public class RehaBuilderImplTest {

  private static final String REQUEST_URI = "/";

  @Test
  public void should_render_resource() throws Exception {

    Reha reha = RehaBuilder.withoutResourceLoader().buildForRequestTo(REQUEST_URI);

    HalResponse response = reha.respondWith(new LinkableTestResource() {

      @Override
      public Link createLink() {
        return new Link(REQUEST_URI);
      }
    }).blockingGet();

    assertThat(response).isNotNull();
    assertThat(response.getContentType()).isEqualTo(HalResource.CONTENT_TYPE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getLink()).isNotNull();
    assertThat(response.getBody().getLink().getHref()).isEqualTo(REQUEST_URI);
    assertThat(response.getBody().hasEmbedded(CARAVAN_METADATA_RELATION));
  }

  @Test
  public void should_render_error_resource_if_exception_is_thrown() throws Exception {

    Reha reha = RehaBuilder.withoutResourceLoader().buildForRequestTo(REQUEST_URI);

    HalResponse response = reha.respondWith(new LinkableTestResource() {

      @Override
      public Link createLink() {
        throw new RuntimeException("failed to create link");
      }
    }).blockingGet();

    assertThat(response).isNotNull();
    assertThat(response.getContentType()).isEqualTo(VndErrorResponseRenderer.CONTENT_TYPE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().hasEmbedded(CARAVAN_METADATA_RELATION));
  }

  @Test
  public void should_return_upstream_entry_point_if_it_exists() {

    TestResourceTree jsonLoader = new TestResourceTree();
    jsonLoader.getEntryPoint().setNumber(123);

    Reha reha = RehaBuilder.withResourceLoader(jsonLoader).buildForRequestTo(REQUEST_URI);

    LinkableTestResource testResource = reha.getEntryPoint(REQUEST_URI, LinkableTestResource.class);

    TestState testState = testResource.getState().blockingGet();
    assertThat(testState).isNotNull();
    assertThat(testState.number).isEqualTo(123);
  }

  @Test
  public void should_throw_HalApiClientException_if_resource_does_not_exists() {

    TestResourceTree jsonLoader = new TestResourceTree();
    Reha reha = RehaBuilder.withResourceLoader(jsonLoader).buildForRequestTo(REQUEST_URI);

    LinkableTestResource testResource = reha.getEntryPoint("/foo", LinkableTestResource.class);

    assertThrows(HalApiClientException.class, () -> testResource.getState().blockingGet());
  }
}
