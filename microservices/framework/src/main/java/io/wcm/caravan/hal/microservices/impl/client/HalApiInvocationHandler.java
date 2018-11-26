/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Handles calls to proxy methods from dynamic proxies created with
 * {@link HalClientProxyFactory#createProxyFromHalResource(Class, HalResource, JsonResourceLoader, RequestMetricsCollector)}
 */
final class HalApiInvocationHandler implements InvocationHandler {

  private static final Logger log = LoggerFactory.getLogger(HalApiInvocationHandler.class);

  private final Single<HalResource> rxContextResource;
  private final Class resourceInterface;
  private final JsonResourceLoader jsonLoader;
  private final RequestMetricsCollector responseMetadata;

  HalApiInvocationHandler(Single<HalResource> rxContextResource, Class resourceInterface, JsonResourceLoader jsonLoader,
      RequestMetricsCollector responseMetadata) {

    this.rxContextResource = rxContextResource;
    this.resourceInterface = resourceInterface;
    this.jsonLoader = jsonLoader;
    this.responseMetadata = responseMetadata;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    // we want to measure how much time is spent for reflection magic in this proxy
    Stopwatch stopwatch = Stopwatch.createStarted();

    // create an object to help with identifiaction of methods and parameters
    HalApiMethodInvocation invocation = new HalApiMethodInvocation(resourceInterface, method, args);

    try {

      // handling of methods annotated with @ResourceProperties
      if (invocation.isResourceProperties()) {

        Maybe<Object> maybeProperties = rxContextResource
            .map(hal -> new ResourceStateHandler(hal))
            .flatMapMaybe(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertReactiveType(maybeProperties, invocation.getReturnType());
      }

      // handling of methods annotated with @RelatedResource
      if (invocation.isRelatedResource()) {

        Observable<Object> rxRelated = rxContextResource
            .map(hal -> new RelatedResourceHandler(hal, jsonLoader, responseMetadata))
            .flatMapObservable(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertReactiveType(rxRelated, invocation.getReturnType());
      }

      throw new RuntimeException("The invoked method is not annotated with " + RelatedResource.class.getSimpleName() +
          " or " + ResourceState.class.getSimpleName());

    }
    // CHECKSTYLE:OFF- we really want to catch any possible runtime exceptions here
    catch (RuntimeException e) {
      // CHECKSTYLE:ON
      throw new RuntimeException("The invocation of " + invocation + " failed", e);
    }
    finally {
      // collect the time spend calling all proxy methods during the current request in the HalResponseMetadata object
      responseMetadata.onMethodInvocationFinished(invocation.toString(), stopwatch.elapsed(TimeUnit.MICROSECONDS));
    }
  }

}
