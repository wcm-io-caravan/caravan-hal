/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.client;

import java.io.InputStream;

import rx.Observable;

@FunctionalInterface
public interface BinaryResourceLoader {

  Observable<InputStream> loadBinaryResource(String uri, RequestMetricsCollector metrics);
}