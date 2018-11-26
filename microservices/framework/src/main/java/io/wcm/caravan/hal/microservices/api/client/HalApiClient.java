/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.api.client;

import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.client.HalApiClientImpl;

/**
 * A generic HAL API client that will provide a dynamic proxy implementation of a given HAL API interface which
 * fetches HAL resources from other SDL services
 */
public interface HalApiClient {

  /**
   * @param uri the absolute path (and query parameters) of the entry point
   * @param halApiInterface the HAL API interface class of a service's resource
   * @return a proxy implementation of the specified entry point interface
   * @param <T> the HAL API interface type
   */
  <T> T getEntryPoint(String uri, Class<T> halApiInterface);

  static HalApiClient create(JsonResourceLoader jsonLoader, BinaryResourceLoader binaryLoader, RequestMetricsCollector collector) {
    return new HalApiClientImpl(jsonLoader, binaryLoader, collector);
  }
}
