/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.api.annotations.ResourceRepresentation;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Handles calls to proxy methods from dynamic proxies created with {@link HalApiClientProxyFactory}
 */
final class HalApiInvocationHandler implements InvocationHandler {

  private final Single<HalResource> rxResource;
  private final Class resourceInterface;
  private final Link linkToResource;
  private final JsonResourceLoader jsonLoader;
  private final RequestMetricsCollector metrics;

  HalApiInvocationHandler(Single<HalResource> rxResource, Class resourceInterface, Link linkToResource,
      JsonResourceLoader jsonLoader, RequestMetricsCollector metrics) {

    this.rxResource = rxResource;
    this.resourceInterface = resourceInterface;
    this.linkToResource = linkToResource;
    this.jsonLoader = jsonLoader;
    this.metrics = metrics;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    // we want to measure how much time is spent for reflection magic in this proxy
    Stopwatch stopwatch = Stopwatch.createStarted();

    // create an object to help with identification of methods and parameters
    HalApiMethodInvocation invocation = new HalApiMethodInvocation(resourceInterface, method, args);

    try {

      if (invocation.isForMethodAnnotatedWithResourceState()) {

        Maybe<Object> maybeProperties = rxResource
            .map(hal -> new ResourceStateHandler(hal))
            .flatMapMaybe(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertReactiveType(maybeProperties, invocation.getReturnType());
      }

      if (invocation.isForMethodAnnotatedWithRelatedResource()) {

        Observable<Object> rxRelated = rxResource
            .map(hal -> new RelatedResourceHandler(hal, jsonLoader, metrics))
            .flatMapObservable(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertReactiveType(rxRelated, invocation.getReturnType());
      }

      if (invocation.isForMethodAnnotatedWithResourceLink()) {

        ResourceLinkHandler handler = new ResourceLinkHandler(linkToResource);
        return handler.handleMethodInvocation(invocation);
      }

      if (invocation.isForMethodAnnotatedWithResourceRepresentation()) {

        Single<Object> rxRepresentation = rxResource
            .map(hal -> new ResourceRepresentationHandler(hal))
            .flatMap(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertReactiveType(rxRepresentation, invocation.getReturnType());
      }

      // unsupported operation
      String annotationNames = ImmutableList.of(RelatedResource.class, ResourceState.class, ResourceLink.class, ResourceRepresentation.class).stream()
          .map(Class::getSimpleName)
          .map(name -> "@" + name)
          .collect(Collectors.joining(", ", "(", ")"));

      throw new UnsupportedOperationException("The method " + invocation + " is not annotated with one of the HAL API annotations " + annotationNames);

    }
    catch (UnsupportedOperationException e) {
      throw e;
    }
    // CHECKSTYLE:OFF- we really want to catch any possible runtime exceptions here to add additional information on the method being called
    catch (RuntimeException e) {
      // CHECKSTYLE:ON
      throw new RuntimeException("The invocation of " + invocation + " has failed with an unexpected exception", e);
    }
    finally {
      // collect the time spend calling all proxy methods during the current request in the HalResponseMetadata object
      metrics.onMethodInvocationFinished(HalApiClient.class, "calling " + invocation.toString(), stopwatch.elapsed(TimeUnit.MICROSECONDS));
    }
  }

}
