/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.api.client;

import java.io.InputStream;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;

@FunctionalInterface
public interface BinaryResourceLoader {

  Single<InputStream> loadBinaryResource(String uri, RequestMetricsCollector metrics);
}