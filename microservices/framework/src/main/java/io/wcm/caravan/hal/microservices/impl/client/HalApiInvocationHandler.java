/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.api.annotations.ResourceRepresentation;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Handles calls to proxy methods from dynamic proxies created with {@link HalApiClientProxyFactory}
 */
final class HalApiInvocationHandler implements InvocationHandler {

  private static final Logger log = LoggerFactory.getLogger(HalApiInvocationHandler.class);

  private final Single<HalResource> rxResource;
  private final Class resourceInterface;
  private final Link linkToResource;
  private final JsonResourceLoader jsonLoader;
  private final RequestMetricsCollector responseMetadata;

  HalApiInvocationHandler(Single<HalResource> rxResource, Class resourceInterface, Link linkToResource,
      JsonResourceLoader jsonLoader, RequestMetricsCollector responseMetadata) {

    this.rxResource = rxResource;
    this.resourceInterface = resourceInterface;
    this.linkToResource = linkToResource;
    this.jsonLoader = jsonLoader;
    this.responseMetadata = responseMetadata;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    // we want to measure how much time is spent for reflection magic in this proxy
    Stopwatch stopwatch = Stopwatch.createStarted();

    // create an object to help with identification of methods and parameters
    HalApiMethodInvocation invocation = new HalApiMethodInvocation(resourceInterface, method, args);

    try {

      // handling of methods annotated with @ResourceState
      if (invocation.isResourceProperties()) {

        Maybe<Object> maybeProperties = rxResource
            .map(hal -> new ResourceStateHandler(hal))
            .flatMapMaybe(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertReactiveType(maybeProperties, invocation.getReturnType());
      }

      // handling of methods annotated with @RelatedResource
      if (invocation.isRelatedResource()) {

        Observable<Object> rxRelated = rxResource
            .map(hal -> new RelatedResourceHandler(hal, jsonLoader, responseMetadata))
            .flatMapObservable(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertReactiveType(rxRelated, invocation.getReturnType());
      }

      // handling of methods annotated with @ResourceLink
      if (invocation.isResourceLink()) {

        ResourceLinkHandler handler = new ResourceLinkHandler(linkToResource);
        return handler.handleMethodInvocation(invocation);
      }

      // unsupported operation
      String annotationNames = ImmutableList.of(RelatedResource.class, ResourceState.class, ResourceLink.class, ResourceRepresentation.class).stream()
          .map(Class::getSimpleName)
          .map(name -> "@" + name)
          .collect(Collectors.joining(", ", "(", ")"));
      throw new RuntimeException("The method must be annotated with one of the HAL API annotations " + annotationNames);

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
