/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.client;

import com.fasterxml.jackson.databind.JsonNode;

import rx.Observable;

@FunctionalInterface
public interface JsonResourceLoader {

  Observable<JsonNode> loadJsonResource(String uri, RequestMetricsCollector metrics);
}