/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.api.client;

import java.io.InputStream;

import org.osgi.annotation.versioning.ConsumerType;

import io.reactivex.Single;

/**
 * An interface to delegate loading of binary resources with any HTTP client
 */
//TODO: either add support for binary resources in HalApiClientImpl or get rid of this interface
@FunctionalInterface
@ConsumerType
public interface BinaryResourceLoader {

  /**
   * @param uri of the requested binary resource
   * @return a {@link Single} providing an {@link InputStream} to read the binary resource
   */
  Single<InputStream> loadBinaryResource(String uri);
}
