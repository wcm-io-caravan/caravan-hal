/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.api.client;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.client.HalApiClientImpl;

/**
 * A generic HAL API client that will provide a dynamic proxy implementation of a given HAL API interface
 * which loads data via HTTP as required when the interface methods annotated with {@link RelatedResource},
 * {@link ResourceState} etc are called.
 */
@ProviderType
public interface HalApiClient {

  /**
   * @param uri the absolute path (and query parameters) of the entry point
   * @param halApiInterface the HAL API interface class of a service's entry point resource
   * @return a proxy implementation of the specified entry point interface
   * @param <T> the HAL API interface type
   */
  <T> T getEntryPoint(String uri, Class<T> halApiInterface);

  /**
   * @param jsonLoader implements the actual loading (and caching) of JSON/HAL resources via any HTTP client library
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance relevant data for the current
   *          incoming request
   * @return an instance of {@link HalApiClient} that should be re-used for all upstream requests required by the
   *         current incoming request
   */
  static HalApiClient create(JsonResourceLoader jsonLoader, RequestMetricsCollector metrics) {
    return new HalApiClientImpl(jsonLoader, metrics);
  }
}
