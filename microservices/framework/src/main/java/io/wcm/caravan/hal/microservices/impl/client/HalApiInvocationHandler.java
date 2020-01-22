/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.api.annotations.ResourceRepresentation;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Handles calls to proxy methods from dynamic proxies created with {@link HalApiClientProxyFactory}
 */
final class HalApiInvocationHandler implements InvocationHandler {


  private final Cache<String, Object> returnValueCache = CacheBuilder.newBuilder().build();

  private final Single<HalResource> rxResource;
  private final Class resourceInterface;
  private final Link linkToResource;
  private final HalApiClientProxyFactory proxyFactory;
  private final RequestMetricsCollector metrics;

  HalApiInvocationHandler(Single<HalResource> rxResource, Class resourceInterface, Link linkToResource,
      HalApiClientProxyFactory proxyFactory, RequestMetricsCollector metrics) {

    this.rxResource = rxResource;
    this.resourceInterface = resourceInterface;
    this.linkToResource = linkToResource;
    this.proxyFactory = proxyFactory;
    this.metrics = metrics;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    // create an object to help with identification of methods and parameters
    HalApiMethodInvocation invocation = new HalApiMethodInvocation(resourceInterface, method, args);

    try {
      return returnValueCache.get(invocation.getCacheKey(), () -> callAnnotationSpecificHandler(invocation));
    }
    catch (UncheckedExecutionException ex) {
      throw ex.getCause();
    }
  }

  private Single<HalResource> addContextToHalApiClientException(Throwable ex, HalApiMethodInvocation invocation) {
    if (ex instanceof HalApiClientException) {
      String msg = "Failed to load an upstream resource that was requested by calling " + invocation;
      return Single.error(new HalApiClientException(msg, (HalApiClientException)ex));
    }
    return Single.error(ex);
  }

  private Object callAnnotationSpecificHandler(HalApiMethodInvocation invocation) {

    // we want to measure how much time is spent for reflection magic in this proxy
    Stopwatch stopwatch = Stopwatch.createStarted();

    try {

      if (invocation.isForMethodAnnotatedWithResourceState()) {

        Maybe<Object> state = rxResource
            .onErrorResumeNext(ex -> addContextToHalApiClientException(ex, invocation))
            .map(hal -> new ResourceStateHandler(hal))
            .flatMapMaybe(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertAndCacheReactiveType(state, invocation.getReturnType(), metrics, invocation.getDescription());
      }

      if (invocation.isForMethodAnnotatedWithRelatedResource()) {

        Observable<Object> relatedProxies = rxResource
            .onErrorResumeNext(ex -> addContextToHalApiClientException(ex, invocation))
            .map(hal -> new RelatedResourceHandler(hal, proxyFactory))
            .flatMapObservable(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertAndCacheReactiveType(relatedProxies, invocation.getReturnType(), metrics, invocation.getDescription());
      }

      if (invocation.isForMethodAnnotatedWithResourceLink()) {

        ResourceLinkHandler handler = new ResourceLinkHandler(linkToResource);
        return handler.handleMethodInvocation(invocation);
      }

      if (invocation.isForMethodAnnotatedWithResourceRepresentation()) {

        Single<Object> representation = rxResource
            .onErrorResumeNext(ex -> addContextToHalApiClientException(ex, invocation))
            .map(hal -> new ResourceRepresentationHandler(hal))
            .flatMap(handler -> handler.handleMethodInvocation(invocation));

        return RxJavaReflectionUtils.convertAndCacheReactiveType(representation, invocation.getReturnType(), metrics, invocation.getDescription());
      }

      // unsupported operation
      String annotationNames = ImmutableList.of(RelatedResource.class, ResourceState.class, ResourceLink.class, ResourceRepresentation.class).stream()
          .map(Class::getSimpleName)
          .map(name -> "@" + name)
          .collect(Collectors.joining(", ", "(", ")"));

      throw new UnsupportedOperationException("The method " + invocation + " is not annotated with one of the HAL API annotations " + annotationNames);

    }
    catch (UnsupportedOperationException e) {
      // these exceptions should just be re-thrown as they are expected errors by the developer (e.g. using invalid types in the signatures of the HAL API interface)
      throw e;
    }
    catch (NoSuchElementException e) {
      // these exceptions should be re-thrown with a better error message
      throw new NoSuchElementException("The invocation of " + invocation + " has failed, "
          + "most likely because no link or embedded resource with appropriate relation was found in the HAL resource");
    }
    // CHECKSTYLE:OFF- we really want to catch any other possible runtime exceptions here to add additional information on the method being called
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
