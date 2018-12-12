/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.api.client;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;

@FunctionalInterface
public interface JsonResourceLoader {

  Single<HalResponse> loadJsonResource(String uri);
}
