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

import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResponseRendererImpl;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Asynchronously creates a {@link HalResponse} from a server-side {@link HalApiInterface} implementation instance,
 * using {@link AsyncHalResourceRenderer} to render a {@link HalResource}, and {@link VndErrorResponseRenderer} to
 * handle any errors that might happen during resource rendering.
 * @see AsyncHalResourceRenderer
 * @see VndErrorResponseRenderer
 */
public interface AsyncHalResponseRenderer {

  /**
   * @param requestUri the URI of the incoming request
   * @param resourceImpl a server-side implementation instance of an interface annotated with {@link HalApiInterface}
   * @return a {@link Single} that emits a {@link HalResponse}
   */
  Single<HalResponse> renderResponse(String requestUri, LinkableResource resourceImpl);

  /**
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance and caching information for
   *          the current incoming request
   * @param exceptionStrategy allows to control the status code and logging of exceptions being thrown during rendering
   * @return a new {@link AsyncHalResponseRenderer} to use for the current incoming request
   */
  static AsyncHalResponseRenderer create(RequestMetricsCollector metrics, ExceptionStatusAndLoggingStrategy exceptionStrategy) {

    AsyncHalResourceRenderer resourceRenderer = AsyncHalResourceRenderer.create(metrics);
    return new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy);
  }
}
