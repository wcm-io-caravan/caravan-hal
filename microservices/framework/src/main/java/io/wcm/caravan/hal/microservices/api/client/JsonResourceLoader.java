/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.api.client;

import io.reactivex.Single;

@FunctionalInterface
public interface JsonResourceLoader {

  Single<JsonResponse> loadJsonResource(String uri);
}
