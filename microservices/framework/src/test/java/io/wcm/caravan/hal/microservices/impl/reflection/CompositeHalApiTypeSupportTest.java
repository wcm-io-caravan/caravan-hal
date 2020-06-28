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
package io.wcm.caravan.hal.microservices.impl.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalApiAnnotationSupport;
import io.wcm.caravan.hal.microservices.api.common.HalApiReturnTypeSupport;
import io.wcm.caravan.hal.microservices.api.common.HalApiTypeSupport;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.impl.client.HalApiClientImpl;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResponseRendererImpl;

@ExtendWith(MockitoExtension.class)
public class CompositeHalApiTypeSupportTest {

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();
  private final ExceptionStatusAndLoggingStrategy exceptionStrategy = null;

  @Mock
  private JsonResourceLoader jsonLoader;

  @Mock
  private HalApiReturnTypeSupport mockReturnTypeSupport;

  @Mock
  private HalApiAnnotationSupport mockAnnotationSupport;

  private HalApiTypeSupportAdapter assertThatCompositeHasDefaultAndAdapter(CompositeHalApiTypeSupport composite) {

    assertThat(composite.getDelegates()).hasSize(2);
    assertThat(composite.getDelegates().get(0)).isInstanceOf(DefaultHalApiTypeSupport.class);
    assertThat(composite.getDelegates().get(1)).isInstanceOf(HalApiTypeSupportAdapter.class);

    return (HalApiTypeSupportAdapter)composite.getDelegates().get(1);
  }

  private void assertThatMockAnnotationSupportIsEffective(HalApiAnnotationSupport annotationSupport) {

    assertThat(annotationSupport).isInstanceOf(CompositeHalApiTypeSupport.class);
    CompositeHalApiTypeSupport composite = (CompositeHalApiTypeSupport)annotationSupport;

    HalApiTypeSupportAdapter adapter = assertThatCompositeHasDefaultAndAdapter(composite);
    assertThat(adapter.getAnnotationSupport()).isSameAs(mockAnnotationSupport);
  }

  private void assertThatMockReturnTypeSupportIsEffective(HalApiTypeSupport typeSupport) {

    assertThat(typeSupport).isInstanceOf(CompositeHalApiTypeSupport.class);
    CompositeHalApiTypeSupport composite = (CompositeHalApiTypeSupport)typeSupport;

    HalApiTypeSupportAdapter adapter = assertThatCompositeHasDefaultAndAdapter(composite);
    assertThat(adapter.getReturnTypeSupport()).isSameAs(mockReturnTypeSupport);
  }

  @Test
  public void client_should_use_custom_return_types() throws Exception {

    HalApiClient client = HalApiClient.create(jsonLoader, metrics, null, mockReturnTypeSupport);

    assertThatMockReturnTypeSupportIsEffective(((HalApiClientImpl)client).getTypeSupport());
  }

  @Test
  public void client_should_use_custom_annotations() throws Exception {

    HalApiClient client = HalApiClient.create(jsonLoader, metrics, mockAnnotationSupport, null);

    assertThatMockAnnotationSupportIsEffective(((HalApiClientImpl)client).getTypeSupport());
  }

  @Test
  public void resource_renderer_should_use_custom_return_types() throws Exception {

    AsyncHalResourceRenderer renderer = AsyncHalResourceRenderer.create(metrics, null, mockReturnTypeSupport);

    assertThatMockReturnTypeSupportIsEffective(((AsyncHalResourceRendererImpl)renderer).getTypeSupport());
  }

  @Test
  public void resource_renderer_should_use_custom_annotations() throws Exception {

    AsyncHalResourceRenderer renderer = AsyncHalResourceRenderer.create(metrics, mockAnnotationSupport, null);

    assertThatMockAnnotationSupportIsEffective(((AsyncHalResourceRendererImpl)renderer).getTypeSupport());
  }

  @Test
  public void response_renderer_should_use_custom_annotations() throws Exception {

    AsyncHalResponseRenderer renderer = AsyncHalResponseRenderer.create(metrics, exceptionStrategy, mockAnnotationSupport, null);

    assertThatMockAnnotationSupportIsEffective(((AsyncHalResponseRendererImpl)renderer).getAnnotationSupport());
  }
}
