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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.mockito.Mockito;

import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.testing.resources.TestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;


public class HalApiClientImplLinkExtractionTest {

  private RequestMetricsCollector metrics;
  private BinaryResourceLoader binaryLoader;
  private TestResourceTree tree;
  private TestResource entryPoint;

  @Before
  public void setUp() {
    metrics = RequestMetricsCollector.create();
    binaryLoader = Mockito.mock(BinaryResourceLoader.class);
    tree = new TestResourceTree();
    entryPoint = tree.getEntryPoint();
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {
    HalApiClientImpl client = new HalApiClientImpl(tree, binaryLoader, metrics);
    T clientProxy = client.getEntryPoint(tree.getEntryPoint().getUrl(), halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }
}
