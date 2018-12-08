/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.api.client;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;

@FunctionalInterface
public interface JsonResourceLoader {

  Single<JsonResponse> loadJsonResource(String uri, RequestMetricsCollector metrics);
}
