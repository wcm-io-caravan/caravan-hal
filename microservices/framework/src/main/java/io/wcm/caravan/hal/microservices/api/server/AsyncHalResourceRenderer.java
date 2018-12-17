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
package io.wcm.caravan.hal.microservices.api.server;

import org.osgi.annotation.versioning.ProviderType;

import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Asynchronously creates a {@link HalResource} representation from a server-side implementation instance of an
 * interface annotated with {@link HalApiInterface}
 */
@ProviderType
public interface AsyncHalResourceRenderer {

  /**
   * @param resourceImpl a server-side implementation instance of an interface annotated with {@link HalApiInterface}
   * @return a {@link Single} that emits a {@link HalResource} which contains the resource state, linked and embedded
   *         resources as defined in the HAL API interface
   */
  Single<HalResource> renderResource(LinkableResource resourceImpl);

  /**
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance and caching information for
   *          the current incoming request
   * @return a new {@link AsyncHalResourceRenderer} to use for the current incoming request
   */
  static AsyncHalResourceRenderer create(RequestMetricsCollector metrics) {
    return new AsyncHalResourceRendererImpl(metrics);
  }
}
