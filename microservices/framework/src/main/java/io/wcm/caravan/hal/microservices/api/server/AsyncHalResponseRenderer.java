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
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResponseRendererImpl;

public interface AsyncHalResponseRenderer {

  Single<HalResponse> renderResponse(String requestUri, LinkableResource resourceImpl);

  static AsyncHalResponseRenderer create(RequestMetricsCollector metrics, ExceptionStatusAndLoggingStrategy exceptionStrategy) {

    AsyncHalResourceRenderer resourceRenderer = AsyncHalResourceRenderer.create(metrics);
    return new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy);
  }
}
